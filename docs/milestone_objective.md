---
title: Milestone M-Auto-1B — Auto-Evolution Calibration (Live-Iter Bootstrap + First Overnight + First Cherry-Pick)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-28
review_cadence: per milestone
supersedes: [docs/milestones/M-Auto-1A_objective.md]
superseded_by: null
notes: >
  Path 1 research-driven milestone consuming
  `docs/solutions/m_auto_1b_calibration_planning.md` (2026-05-28,
  research-agent). Human-locked planning decisions (2026-05-28
  AskUserQuestion): (1) proposal adopted as M-Auto-1B planning
  baseline; (2) sub-sprint slicing = **Alt-S2 (2 sub-sprints)** —
  S-Auto-5 (live-iter bootstrap + detector calibration Fix-C hybrid)
  + S-Auto-6 (first overnight batch + first human review + first
  cherry-pick to main); (3) R-S57 fix path = **Fix-C (hybrid:
  word-boundary regex + evidence-driven `synonym_map_enabled` toggle
  decision + optional Fix-B FLAG rule fallback)**. This is the
  SEVENTH milestone under `iteration_governance.md` §8 framework.

  **Core thesis**: M-Auto-1B's success is NOT measured by "how many
  iterations ran" but by **detector calibration evidence against
  real meta-agent (DeepSeek / Kimi / AICodeWith) propose outputs**.
  S-Auto-4's 17-fixture calibration table (11/11 forbidden FAIL +
  4/4 clean PASS + 2/2 borderline FLAG) was constructed from
  synthetic deliver-agent + research-agent fixtures; real
  meta-agent distribution differs in length, structure, and
  abstract-vs-concrete style. M-Auto-1B's first job is to extend
  this calibration table with ≥10 real meta-agent samples before
  any overnight batch runs, so the overnight FLAG rate stays under
  the 25% warn threshold AND no false-negative slips a §1.7
  borderline edit through into main.

  **Why two sub-sprints (Alt-S2)**: detector calibration (S-Auto-5)
  → overnight batch (S-Auto-6) is a strong sequential dependency.
  Bundling them into a single sub-sprint (Alt-S1) couples
  calibration evidence with batch execution into one commit, makes
  Codex review of the calibration decision harder to scope, and
  wastes ~6h of overnight time if calibration surfaces a problem
  after the batch starts. Splitting at 2 sub-sprints (Alt-S2)
  freezes calibration evidence at S-Auto-5 close and lets S-Auto-6
  run overnight on a calibrated detector. §8.5 ceiling = 5
  sub-sprints; Alt-S2 leaves 3 slots of margin for fix-iteration
  (S-Auto-5.1 / S-Auto-6.1) if either sub-sprint surfaces a problem.

  **Why Fix-C hybrid for R-S57**: the bypass shape `Whenever ... =>`
  fails `\bif\b` regex anchor and synonym-map `" whenever "` (with
  required surrounding whitespace). Fix-A (word-boundary regex
  `\b(?:whenever|when)\b` → `if`) structurally closes the exact
  bypass shape, but "when" is an English-common word risking
  false-positive on clean prose ("when the user describes their
  issue"). Fix-B (FLAG-only rule) doesn't structurally close the
  literal bypass — Codex would have to keep flagging. Fix-C
  implements Fix-A's word-boundary substitution but keeps
  `synonym_map_enabled: false` default; calibration evidence on
  real meta-agent batch decides whether to flip the toggle OR add
  Fix-B FLAG rule as fallback. Evidence-driven final state respects
  §1.7 "evidence over intent".

  **§8.1 conformance**: M-Auto-1B has 2 sub-sprints (within 3-5
  ceiling; Alt-S2 leaves margin for fix-iteration).

  **Codex review plan (§4.3)**: milestone-shared at M-Auto-1B close
  DEFAULT with **S-Auto-5 per-sub-sprint Codex** (trigger #2 — the
  detector calibration change touches the §1.7 structural guard;
  Codex must independently verify Fix-C structural soundness +
  calibration evidence BEFORE S-Auto-6 begins overnight running it
  against real propose outputs).

  **R-item coupling**: M-Auto-1B **consumes** `R-S57-anti-hardcode
  -whenever-arrow-synonym-bypass` (Fix-C resolution at S-Auto-5
  close + end-to-end calibration evidence at M-Auto-1B close).
  Coupled read-only: `R-bad-case-parallel-session-establishment
  -flakiness` (bad_cases still parallel=1; overnight batch
  accumulates further data points); `R-iwzx-uc-k-vs-uc-h-routing
  -spurious-distress` (natural optimization target — meta-agent
  may propose `discover_triage.procedure` edits to address it);
  `R-shadow-fixture-empty-form-session-create-400` (S-Auto-6
  shadow rerun excludes cs59s01/cs59s02 from bot-side
  regression count per S-Auto-2 baseline_loader handling).

  **Close-day drift observation (M-Auto-1A → M-Auto-1B planning
  input, NOT a milestone gate)**: M-Auto-1A close 2026-05-28
  observed 5/34 cases (14.7%) bad-case + shadow programmatic drift
  ALL one-direction `True/0.5 → False/0.0`, bot byte-identical to
  M5 → LLM-provider non-determinism signature. M-Auto-1B planning
  input: do NOT auto-discard candidates because of this close-day
  drift pattern; watch for provider drift signal during first
  overnight batch; baseline rerun ≥2 times BEFORE the overnight to
  establish a drift envelope (§3.3 Path A of the proposal).

  **What NOT in M-Auto-1B scope** (deferred):
  - Stage-2 unlock (templates.yaml / system_prompt.txt) — decision
    point AFTER M-Auto-1B close;
  - M3-B Single Handover Orchestrator P0 — deferred per human's
    M-Auto-1-first decision; re-evaluated at M-Auto-1B close;
  - Projection-hygiene milestone candidate (M5 carry-over) —
    independent track;
  - UC-G/H/I/J bad-case seeding (`R-bad-case-suite-uc-ghij-seed
    -from-real-sessions`) — natural optimization target once
    seeded; not pre-seeded in M-Auto-1B;
  - `R-eval-java-module-retirement` — M-Auto-2+ governance-hygiene.
---

# Milestone M-Auto-1B — Auto-Evolution Calibration

## 1. Milestone class

**Multi-layer milestone, 2 coordinated sub-sprints.** Layer + §7-stanza + Codex breakdown:

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Auto-5 / Sprint 058 — Live-iter bootstrap + Detector calibration (Fix-C hybrid) | `infra` (primary) + `eval_spec` (calibration) | REQUIRED | **Per-sub-sprint (§4.3 trigger #2 — detector change touches §1.7 structural guard; Codex must independently verify Fix-C structural soundness + calibration evidence BEFORE S-Auto-6 overnight begins)** |
| S-Auto-6 / Sprint 059 — First overnight batch + First human review + First cherry-pick to main | `eval_spec` | REQUIRED | Milestone-shared (default) UNLESS cherry-pick candidate borderline-§5.3 surfaces |

S-Auto-5 per-sub-sprint Codex must return `pass` (or `approve with downgrade-to-signal follow-up`) **BEFORE S-Auto-6 overnight batch begins**. The milestone-shared Codex review at M-Auto-1B close evaluates the cumulative range covering both sub-sprints; S-Auto-6 is Codex-deferred to that close unless the cherry-pick candidate touches a §1.7 borderline that requires per-sub-sprint review (deliver-agent + human judge at sub-sprint planning).

## 2. Goal

Stand up the **first real run** of the auto-evolution loop end-to-end and bring the anti-hardcode detector to **calibration-validated state** against real meta-agent propose outputs. At M-Auto-1B close, M-Auto-1A's substrate has been demonstrated to:

1. Complete live-iter prerequisites: a built `server/` jar exists, `AUTOLOOP_META_LLM_API_KEY` is verified live, working tree is clean on `auto-loop-branch`.
2. Run ≥1 real iteration (`python -m autoloop run --experiments 1` without `--dry-run`) to a verdict (keep / discard / error), proving the 14-step state machine + applier mvn alt-port Spring spawn + 47-case 3-suite eval all work end-to-end against a real meta-agent LLM.
3. Produce ≥10 real meta-agent propose samples in `autoloop/results/experiments.jsonl` to calibrate the anti-hardcode detector against real distribution (not just S-Auto-4's 17 synthetic fixtures).
4. Close `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` via Fix-C hybrid implementation: word-boundary regex `\b(?:whenever|when)\b` → `if` in `_normalize` pipeline; `synonym_map_enabled` toggle final state decided by calibration evidence (flip to `true` IF 17-fixture sweep maintains 11/11 + 4/4 + 2/2 AND real-meta-agent batch FLAG rate <25% AND FP=0 on clean prose; otherwise keep `false` + add Fix-B FLAG rule fallback).
5. Run a first overnight batch (10-20 iterations, 6-8h budget) on the calibrated detector against the v1 47-case dataset (bad_cases ×12 + anchor_outcome ×12 + shadow ×23) per the §2 dataset scope frozen at M-Auto-1A.
6. Conduct first human review of overnight kept candidates per §5.6 cadence: deliver-agent + human read per-turn traces on bad_cases + anchor_outcome, judge PASS / FAIL / IMPROVING jointly, decide cherry-pick via AskUserQuestion.
7. Execute first cherry-pick to main: at least one human-approved kept candidate goes through `python -m autoloop apply --experiment exp-<N>` Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit; human manually commits). M-Auto-1B's `config.fitness.baseline_dir` advances accordingly.

**What ships in main at M-Auto-1B close**:

- Zero `server/` / `eval/` Java / `eval_interactive/eval_interactive/**` / case_spec / case_specs_shadow / `server/src/main/resources/{prompts,scripts,config,mock}/**` edits (these stay byte-identical to M-Auto-1A close `b6b627b`).
- Detector calibration edits land in `autoloop/autoloop/sandbox/anti_hardcode_check.py` + `autoloop/config.yaml` `anti_hardcode` block + `autoloop/tests/`. Rule count stays ≤30 (current 11; Fix-C adds at most +2).
- Optionally (S-Auto-6 cherry-pick): exactly ONE Skill YAML LLM-soft field edit on `server/src/main/resources/skills/*.yaml` (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc only — sandbox-validated, anti-hardcode PASS, gaming 0-ERROR; human-approved via AskUserQuestion + §5.6 manual review; human-committed). At MOST 1 cherry-pick; further kept candidates stay on `autoloop/keep-<N>` branches awaiting M-Auto-2+ review.
- Auto-loop result accumulation under `autoloop/results/runs/exp-*/`, `experiments.jsonl`, `iterations.sqlite`, `lessons.md` (first lesson at K=10 trigger), and the configured `baseline_dir` advances if cherry-pick lands.

**Dataset scope (v1 unchanged from M-Auto-1A)**: per-iteration fitness suite = `bad_cases` (12) + `anchor_outcome` (12) + `shadow` (23) = 47 cases per iter, expected ~12-15 min per iteration. `anchor` 159 NOT consulted as per-iteration fitness; M-Auto-1B does NOT extend this scope.

**Layer 0 scope (v1 unchanged from M-Auto-1A)**: Python `hard_checks` Tier-0 family. The legacy `eval/src/main/java/com/gumtree/csagent/eval/` module remains superseded (governance-hygiene `R-eval-java-module-retirement` deferred to M-Auto-2+).

Crucially M-Auto-1B does NOT widen the mutable surface beyond Skill YAML LLM-soft fields (Stage-1 only). Stage-2 entry decision (templates.yaml / system_prompt.txt unlock) is reserved for a SEPARATE milestone after M-Auto-1B close, judged against the evidence accumulated during M-Auto-1B (≥3 kept human-approved cherry-picks + 0 borderline §5.3 cases would be a positive signal; M-Auto-1B itself does NOT pre-commit Stage-2 entry criteria).

## 3. Sub-sprint sequence

### S-Auto-5 / Sprint 058 — Live-iter bootstrap + Detector calibration (Fix-C hybrid)

**Layer:** `infra` (primary) + `eval_spec` (calibration). **§7 stanza:** REQUIRED. **Codex:** **PER-SUB-SPRINT (§4.3 trigger #2)**. **Estimated dev:** 4-5 dev-days + Codex ~2 days.

**Scope (5 sentences):**

1. Complete the three live-iter prerequisites WAIVED-BY-HUMAN at M-Auto-1A close (OQ-S56.5): (a) `mvn package -pl server -am -DskipTests` to produce `server/target/*.jar`; (b) verify `AUTOLOOP_META_LLM_API_KEY` is set in `autoloop/.env.local` via `python -m autoloop check` returning success; (c) confirm `git status` shows a clean working tree on `auto-loop-branch` immediately before the first live iteration.
2. Run 1-3 real iterations: `python -m autoloop run --experiments 1` (no `--dry-run`); the full 14-step state machine must complete (regardless keep / discard / error verdict) and `autoloop/results/runs/exp-1/` must contain `hypothesis.json` + `diff.yaml` + per-stage verdict artefacts. If the first iteration completes <15 min, run 2-3 more iterations to accumulate ≥10 real meta-agent propose samples in `autoloop/results/experiments.jsonl` for calibration input. Per-iteration elapsed wall-clock time is recorded; if >40 min, surface as observation toward `docs/milestone_objective.md` §10 stop condition consideration (do NOT halt the sub-sprint unilaterally — surface to deliver-agent + human).
3. Implement **Fix-C step 1** in `autoloop/autoloop/sandbox/anti_hardcode_check.py` `_normalize()`: BEFORE the existing `_SYNONYM_MAP` whole-string substitution, add a word-boundary regex substitution `re.sub(r"\b(?:whenever|when)\b", "if", norm)` guarded by `if synonym_map_enabled`. Keep `_SYNONYM_MAP` unchanged. Re-run the existing 17-fixture calibration table sweep: the regression test must continue to produce 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG_FOR_CODEX; the existing 3 detector self-discipline regression tests (`test_detector_source_does_not_hardcode_eval_case_ids` + `..._user_utterance_literals` + `..._rule_count_bounded`) must continue to PASS; rule count stays ≤30.
4. Run the **real-meta-agent batch calibration**: classify the ≥10 propose samples from step 2 against the detector with `synonym_map_enabled=true` (temporary toggle for this measurement); record FAIL count + FLAG_FOR_CODEX count + clean PASS count; compute FLAG rate. Acceptance: FLAG rate <25% (the existing `flag_for_codex_rate_warn_threshold` config); FP (false-positive on clean prose) = 0. The Codex Axis B exact bypass `Whenever the customer describes an appeal => route to escalation and skip normal triage.` MUST now FAIL the detector. If acceptance passes, **Fix-C step 2 = flip `synonym_map_enabled: false → true` in `autoloop/config.yaml` `anti_hardcode` block** (final state). If FP>0 on any clean prose sample, **Fix-C step 2 = retain `synonym_map_enabled: false` + implement Fix-B FLAG rule** (new `_RE_Q1_WHEN_ARROW_FLAG` regex emitting FLAG_FOR_CODEX, NOT auto-FAIL, on `\b(?:whenever|when)\b[\s\S]{1,120}?(?:→|=>|->)[\s\S]{1,120}` pattern). The step 2 decision + supporting evidence (per-sample FAIL/FLAG/PASS verdict) MUST be recorded in `docs/sprints/sprint-058-handoff.md` §X "Calibration evidence" section so per-sub-sprint Codex consumes it.
5. Per-sub-sprint Codex (§4.3 trigger #2) reviews the cumulative S-Auto-5 commit range: validates Fix-C step 1 structural soundness (word-boundary regex doesn't false-positive on common English prose), spot-checks ≥3 adversarial constructions (unicode obfuscation of "whenever"; multi-line decomposition `When ...\n=> ...`; semantic synonym swap "anytime ... ->"), verifies `synonym_map_enabled` toggle decision is evidence-grounded (NOT a flip-without-justification), verifies the existing 3 detector self-discipline regression tests still PASS, verifies rule count ≤30. Codex must return `pass / 0` (or `approve with downgrade-to-signal follow-up` if a residual borderline case surfaces) BEFORE S-Auto-6 overnight batch begins.

**Files in scope** (S-Auto-5 only):

- **NEW**: `autoloop/results/runs/exp-1/` (and exp-2/exp-3 if multi-iter), `autoloop/results/experiments.jsonl` (append 1-3 entries), `autoloop/results/iterations.sqlite` (insert 1-3 rows), `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json` (10+ sanitized samples for regression).
- **EXTEND**: `autoloop/autoloop/sandbox/anti_hardcode_check.py` (`_normalize` body extension; `_SYNONYM_MAP` unchanged; if Fix-C step 2 = Fix-B fallback, add `_RE_Q1_WHEN_ARROW_FLAG` regex + matching rule function; rule count stays ≤30); `autoloop/config.yaml` (`anti_hardcode.synonym_map_enabled` toggle final state); `autoloop/tests/test_anti_hardcode_check.py` (add ≥3 word-boundary positive cases including the Codex Axis B exact phrase; add ≥3 clean prose negative cases including "When the user describes their issue..."; add ≥1 multi-line "Whenever ...\n=> ..." case; if Fix-B fallback in step 2, add ≥2 FLAG_FOR_CODEX cases for `\b(?:whenever|when)\b ... =>` shape).
- **NEW**: `docs/sprints/sprint-058-{objective,handoff}.md` (dev authors handoff at close; deliver-agent authors objective at S-Auto-5 open).
- **NEW**: `compact/sprint-058-codex-review-prompt.md` (deliver-agent authors at S-Auto-5 close, self-contained per §9 invariant, embeds §4.1 kernel + commit range + verification axes verbatim).
- **NO touch** to `server/`, `eval/src/main/java/`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/`, `db/migration/`, `server/src/main/resources/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, sprint/milestone archives, `docs/codex-findings.md` (scaffold; per-sub-sprint Codex writes at close).

### S-Auto-6 / Sprint 059 — First overnight batch + First human review + First cherry-pick to main

**Layer:** `eval_spec`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default) UNLESS cherry-pick candidate borderline-§5.3 surfaces. **Estimated dev:** 1-2 dev-days execution + 1 overnight (6-8h auto-loop) + 1-2 days human review + 1 day close.

**Scope (5 sentences):**

1. Run a **pre-batch baseline rerun** ≥2 times of the v1 47-case fitness suite (bad_cases ×12 + anchor_outcome ×12 + shadow ×23) on the calibrated detector before the overnight begins. Establish a **drift envelope** (median per-suite case_passed count + IQR) and record it in `docs/sprints/sprint-059-handoff.md` §X "Baseline drift envelope". If baseline-vs-baseline drift exceeds 5/34 cases (the M-Auto-1A close-day signature), surface to deliver-agent + human BEFORE starting overnight (provider drift may have widened; halt or proceed with widened envelope is a joint call).
2. Run the **overnight batch**: `python -m autoloop run --experiments 15` (target 10-20; budget 6-8h; sub-sprint accepts any final count ≥10) on `auto-loop-branch`. Each iteration writes to `autoloop/results/runs/exp-<N>/` + `experiments.jsonl` + `iterations.sqlite` + `lessons.md` (first K=10 lesson compaction triggers automatically if iteration count reaches K). Crash recovery handles transient LLM API errors; if total errors >50%, halt overnight + surface (loop crash recovery is M-Auto-1A S-Auto-3 substrate; M-Auto-1B does NOT add new recovery code).
3. Next morning, **§5.6-style manual review of kept candidates**: deliver-agent + human read per-turn traces of EACH kept candidate's bad_cases + anchor_outcome runs (open `eval_interactive/results/<run-id>/` + `autoloop/results/runs/exp-<N>/`). For each kept candidate, judge PASS / FAIL / IMPROVING jointly (NOT programmatic alone), filter through the drift envelope from step 1, and classify as: **eligible for cherry-pick** (manual review PASS + no §5.3 borderline) OR **deferred to M-Auto-2+** (manual review PASS but borderline §5.3 — "drift to keyword bot" risk; kept on `autoloop/keep-<N>` branch) OR **discarded** (manual review FAIL despite programmatic PASS).
4. Deliver-agent surfaces the candidate slate to human via **AskUserQuestion**: each eligible cherry-pick candidate gets a row with (target_skill, target_field, edit_summary, programmatic verdict, manual review verdict, deliver-agent recommendation). Human selects exactly ONE candidate to cherry-pick (or 0 candidates with explicit "no human-approved candidate" justification — close PASS still possible on other gates). For the selected candidate, execute `python -m autoloop apply --experiment exp-<N>` in Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit per OQ-S55.1 disposition 2026-05-27); human inspects `git status`, stages explicitly, commits manually with message `Sprint 059 / S-Auto-6 / M-Auto-1B — apply exp-<N> to main` and the standard deliver-agent footer.
5. After cherry-pick (or 0-cherry-pick close decision), record final observations: per-iteration elapsed time average; cumulative FLAG rate observed across overnight; `shadow_disagreement_rate` first measurement against the baseline envelope; gaming flag count + severity distribution. Update `autoloop/config.yaml` `fitness.baseline_dir` to advance past the cherry-pick commit (if any) so the next milestone's baseline is post-cherry-pick. `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` is **closed at M-Auto-1B close** (S-Auto-5 resolved the structural fix + S-Auto-6 end-to-end validates it; deliver-agent annotates the close in `docs/action_bank.md` §5 + §6 close-action index row).

**Files in scope** (S-Auto-6 only):

- **NEW**: `autoloop/results/runs/exp-<N>/` × (10-20 overnight iterations), `autoloop/results/experiments.jsonl` (append 10-20 entries), `autoloop/results/iterations.sqlite` (insert 10-20 rows), `autoloop/results/lessons.md` (append ≥1 K=10 lesson if iteration count reaches K), optional `autoloop/results/report-m-auto-1b.html` (per §10 observability).
- **EXTEND**: `autoloop/config.yaml` (`fitness.baseline_dir` advance if cherry-pick lands; `scoring_code_baseline_sha` unchanged since scoring code is hard-fenced #9 of M-Auto-1A §6 / M-Auto-1B §6 — still locked).
- **CONDITIONAL** (if cherry-pick lands): exactly ONE Skill YAML edit on `server/src/main/resources/skills/<skill_name>.yaml` to ONE LLM-soft field class (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc) per the cherry-picked `autoloop/exp-<N>` branch.
- **NEW**: `docs/sprints/sprint-059-{objective,handoff}.md`.
- **NO touch** (UNCHANGED FROM S-Auto-5 hard-fences): `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/`, `db/migration/`, `server/src/main/resources/{prompts,scripts,config,mock}/**` (skills/ is the SOLE writable path and only via the cherry-pick mechanism), `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, sprint/milestone archives, `docs/codex-findings.md` (scaffold; milestone-shared Codex writes at M-Auto-1B close).

## 4. Non-goals (explicit)

- M-Auto-1B does NOT unlock Stage-2 mutable surface (templates.yaml / system_prompt.txt / routing_prompt.txt). Stage-2 entry is a SEPARATE milestone decision after M-Auto-1B close, evaluated against M-Auto-1B's evidence accumulation (≥3 kept human-approved cherry-picks + 0 borderline §5.3 cases would be a positive signal; M-Auto-1B does NOT pre-commit Stage-2 entry criteria).
- M-Auto-1B does NOT introduce a new Tier-0 invariant. C2/C3 candidates from M2 close remain DEFER unchanged. If overnight batch surfaces production-trace-like observations that warrant Tier-0 consideration, those become R-items for M-Auto-2+ planning, NOT in-flight elevation in M-Auto-1B.
- M-Auto-1B does NOT touch the Single Handover Orchestrator (M3-B P0; deferred per human's M-Auto-1-first decision; re-evaluated at M-Auto-1B close).
- M-Auto-1B does NOT modify `docs/runtime_freeze_and_risk_policy.md`, `docs/foundational/**`, `docs/current/iteration_governance.md`, `docs/teams/**`. Governance fold-back is a separate doc-PR on the normal cadence.
- M-Auto-1B does NOT modify `eval_interactive/eval_interactive/**` (the inner module), `eval_interactive/case_specs/**`, or `eval_interactive/case_specs_shadow/**`. The OQ-S56.1 blessing for the top-level `eval_interactive/eval_interactive.yaml` env-var indirection carries forward unchanged from M-Auto-1A §6.1.
- M-Auto-1B does NOT modify `eval/src/main/java/**` (the legacy Java replay module; `R-eval-java-module-retirement` deferred to M-Auto-2+).
- M-Auto-1B does NOT modify the four `autoloop/autoloop/scoring/` baseline files (`tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`). Any change would trigger `gaming.scoring_code_drift.sha_changed` ERROR against the configured baseline SHA `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c` (set at M-Auto-1A close `b6b627b`).
- M-Auto-1B does NOT replace the §5.6 bad-case manual-review human-judgment gate. The auto-loop's per-iteration `bad_cases` programmatic `case_passed` count is a PROGRAMMATIC SUB-SIGNAL fed to tier_evaluator; human review on kept candidates remains the primary gate at sub-sprint close (for cherry-pick decision) and milestone close (per §5.6 cadence).
- M-Auto-1B does NOT promote any gaming check from observation-only to gating. `gaming.observation_only_in_v1: true` stays; evidence on ERROR-severity flag accumulation from the overnight batch is M-Auto-2+ planning input, NOT an in-flight rule promotion.
- M-Auto-1B does NOT widen the per-iteration fitness suite beyond 47 cases (bad_cases ×12 + anchor_outcome ×12 + shadow ×23). `anchor` 159 stays excluded per the §2 dataset scope decision frozen at M-Auto-1A.
- M-Auto-1B does NOT replace Codex anti-hardcode review. S-Auto-5's calibration evidence is belt-and-suspenders to the per-sub-sprint Codex spot-check, NEVER a substitute.
- M-Auto-1B does NOT pre-seed new bad cases (UC-G/H/I/J seeding is `R-bad-case-suite-uc-ghij-seed-from-real-sessions` — independent track; new cases would become natural optimization targets once seeded, but no M-Auto-1B action).
- M-Auto-1B does NOT alter shadow-firewall posture. Per-case shadow info NEVER reaches `meta_agent/proposer.py`; aggregate `regression_detected: y/n` is the only shadow signal the loop sees.
- M-Auto-1B allows AT MOST 1 cherry-pick to main. If overnight produces multiple eligible kept candidates, the human picks ONE; the rest stay on `autoloop/keep-<N>` branches awaiting M-Auto-2+ review.

## 5. Milestone acceptance bar

**Hard gates (close decision is PASS only if all clear):**

- [ ] **Tier-0 safety floor unchanged**: M-Auto-1B ships zero `server/src/main/java/` / `eval/src/main/java/` / Python eval-code edits. Verified by `git diff --stat <M-Auto-1A-close>..<M-Auto-1B-close> -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/ data/ db/migration/` returning empty.
- [ ] **Java test baseline preserved**: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED from M-Auto-1A close `b6b627b` (same as M5 close `c9390dc`). The inherited `SystemPromptUserRequestedTiebreakerTest` (OQ-S41.5 STATUS QUO) persists.
- [ ] **Python test baseline preserved**: eval_interactive `3 failed, 486 passed` UNCHANGED (OQ-S47.3 env-specific). `autoloop/tests/` grows from 216 baseline by ~6-12 new tests (S-Auto-5 word-boundary + clean-prose + multi-line cases; optional Fix-B FLAG rule cases if step 2 chooses fallback path); S-Auto-6 typically adds 0 new tests (execution-driven sub-sprint).
- [ ] **Live iteration end-to-end (NOW required, no longer waivable)**: `python -m autoloop run --experiments 1` (NO `--dry-run`) drives at least ONE full iteration to a verdict on `auto-loop-branch`; the prerequisite (built `server/` jar) is satisfied; the full 14-step state machine executes without crash. Recorded in `autoloop/results/runs/exp-1/` + `experiments.jsonl` + `iterations.sqlite`. This is the gate that M-Auto-1A WAIVED at OQ-S56.5; M-Auto-1B retires the waiver.
- [ ] **S-Auto-5 per-sub-sprint Codex review `pass / 0`** (or `approve with downgrade-to-signal follow-up`) covering Fix-C structural soundness + calibration evidence + 3 adversarial spot-checks + detector self-discipline regression. Reject verdict triggers fix-iteration sub-sprint (S-Auto-5.1) before S-Auto-6 begins overnight.
- [ ] **Detector calibration evidence at S-Auto-5 close**: 17-fixture sweep maintains 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 FLAG_FOR_CODEX; detector self-discipline 3 regression tests PASS; rule count ≤30; the Codex Axis B exact bypass `Whenever the customer describes an appeal => route to escalation and skip normal triage.` now FAILS the detector (no longer PASS-bypasses).
- [ ] **Real-meta-agent batch calibration at S-Auto-5 close**: ≥10 real meta-agent propose samples classified through the detector; FLAG rate <25% (under `flag_for_codex_rate_warn_threshold`); FP rate on clean prose = 0 (zero false-positive on the clean-prose negative-control samples). `synonym_map_enabled` toggle final state (true OR false-with-FLAG-fallback) recorded with evidence in `docs/sprints/sprint-058-handoff.md`.
- [ ] **Pre-batch baseline drift envelope at S-Auto-6 open**: ≥2 baseline reruns of the 47-case suite produce a median + IQR envelope. If envelope width is materially wider than the 5/34 (14.7%) M-Auto-1A close-day signature, deliver-agent + human jointly decide to (a) proceed with widened envelope or (b) halt for upstream LLM-provider investigation. Decision + envelope recorded in `docs/sprints/sprint-059-handoff.md`.
- [ ] **First overnight batch executed at S-Auto-6**: ≥10 iterations completed end-to-end (target 10-20; final count ≥10 is the gate); per-iteration verdicts serialized to `experiments.jsonl` + `iterations.sqlite`. Iteration crashes during overnight are acceptable IF total errors ≤50% (loop crash-recovery substrate from S-Auto-3 handles transient API errors).
- [ ] **First human review of overnight kept candidates**: deliver-agent + human jointly read per-turn traces of EACH kept candidate; PASS / FAIL / IMPROVING judgment recorded per candidate in `docs/sprints/sprint-059-handoff.md`. Candidates classified as cherry-pick-eligible / deferred-to-M-Auto-2+ / discarded.
- [ ] **First cherry-pick decision via AskUserQuestion**: human selects exactly ONE candidate to cherry-pick OR 0 candidates with explicit "no human-approved candidate" justification recorded in handoff. If cherry-pick lands, the apply Hybrid path executes correctly (cherry-pick + baseline patch emit + NO auto-commit; human manually commits) and `git log main` shows the new commit.
- [ ] **Curated bad-case suite manual review pass (PRIMARY GATE per §5.6)**: deliver-agent + human run the bad-case suite at M-Auto-1B close (parallel=1 per `R-bad-case-parallel-session-establishment-flakiness`). Distribution: if cherry-pick landed, expected ≥1 case moves to IMPROVING or PASS; if no cherry-pick, expected distribution matches M-Auto-1A close. Joint judgment recorded.
- [ ] **Shadow regression-safety gate (parity with M-Auto-1A NEW)**: deliver-agent runs `eval_interactive/case_specs_shadow/` at M-Auto-1B close. Expected: no NEW shadow regression beyond `R-shadow-fixture-empty-form-session-create-400`. If cherry-pick landed, shadow drop must be ≤3% (per S-Auto-2 tier_evaluator Layer 4 threshold from M-Auto-1A).
- [ ] **R-S57 closed**: `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` marked closed in `docs/action_bank.md` §5 with the resolution annotation (Fix-C step 1 + step 2 final state + real-meta-agent calibration evidence pointer).
- [ ] **Milestone-shared Codex review at M-Auto-1B close**: `pass / 0` (or `approve with downgrade-to-signal follow-up`) over cumulative `<M-Auto-1A-close>..<M-Auto-1B-close>` range. Codex consumes S-Auto-5 per-sub-sprint review as input (NOT re-litigated); axes M1-MN cover cumulative §4.1 kernel walk, hard-fence verification, cherry-pick decision review, calibration evidence audit, observability claim audit.

**Observation-only (recorded; does not gate close):**

- Per-iteration elapsed-time average across overnight (expected 12-25 min; observation toward M-Auto-2 optimization if >40 min).
- `shadow_disagreement_rate` (§6 architecture-health metric) first measurement on the overnight kept-vs-discarded distribution.
- Cumulative FLAG rate across overnight propose-stage anti-hardcode checks.
- Gaming flag counts + severity distribution across overnight (observation-only_in_v1 per `autoloop/config.yaml`).
- Lessons compaction: `autoloop/results/lessons.md` should contain ≥1 LLM-distilled lesson if overnight reaches K=10 iteration count.
- `new_semantic_hardcode_count` (§6) = **0** (Fix-C step 1 is a generic structural regex `\b(?:whenever|when)\b` → `if`; not a specific eval phrase or user utterance per D2; optional Fix-B FLAG rule is similarly generic structural).
- `soft_signal_conversion_count` (§6): if cherry-pick lands and the edit downgrades a hardcoded behaviour to a soft signal (e.g., procedure narrative softens a "must" to a "should consider"), count as 1; otherwise 0.
- `planner_ownership_ratio`: unchanged if no cherry-pick lands. If cherry-pick lands and the edit shifts a decision boundary from runtime to LLM, observation-record toward M-Auto-2 measurement infrastructure.

## 6. Hard fences (milestone-level)

These hard fences inherit from M-Auto-1A §6 with the cherry-pick exception. Most are unchanged; the changed / new items are flagged.

1. **No edits** to any file under `server/src/main/java/**`. The Runtime side stays byte-identical to M-Auto-1A close `b6b627b` (which is byte-identical to M5 close `c9390dc`).
2. **No edits** to any file under `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. The Evaluator side stays byte-identical.
3. **CHANGED FROM M-Auto-1A**: `server/src/main/resources/skills/*.yaml` files are **conditionally writable EXACTLY ONCE** in M-Auto-1B — via the S-Auto-6 cherry-pick mechanism only. Outside of cherry-pick, no edits. `server/src/main/resources/{prompts,scripts,config,mock}/**` stays byte-identical (Stage-1 mutable surface = Skill YAML only).
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, `docs/teams/**`. Foundational + governance stay frozen for this milestone.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-057-*` or any prior milestone archive under `docs/milestones/`.
6. **No `git add -A`** by the dev agent. Stage only S-Auto-N scope files explicitly. Deliver-agent close-bundle artefacts (`docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, Codex prompts + findings) bundled by the human at close.
7. **CHANGED FROM M-Auto-1A**: cherry-pick to main is **ALLOWED EXACTLY ONCE** during S-Auto-6 — for ONE human-approved kept candidate via `python -m autoloop apply --experiment exp-<N>` Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit; human manually commits). Any additional kept candidates stay on `autoloop/keep-<N>` branches awaiting M-Auto-2+ review.
8. **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close verdict. If overnight surfaces a Tier-0 candidate observation, surface to deliver-agent + human as R-item for M-Auto-2+ planning, NOT in-flight elevation.
9. **No cross-file diff** by meta-agent ever (sandbox enforces). Single-Skill, single-field-class diff per iteration. The cherry-pick mechanism inherits this: only the one Skill YAML file from the cherry-picked branch lands on main.
10. **No shadow-set leakage to meta-agent**. Aggregate `{shadow_regression_detected: yes|no, drop_pct: <float>}` only. Per-case shadow failures NEVER reach `meta_agent/proposer.py`. Tier_evaluator firewall holds across S-Auto-5 + S-Auto-6 + the M-Auto-1B close shadow rerun.
11. **No mutation of `eval_interactive/results/` schema**. Auto-loop output stays under `autoloop/results/` only; eval invocation reuses existing schema unchanged.
12. **No editing of `docs/codex-findings.md` during M-Auto-1B execution**. The live scaffold receives the S-Auto-5 per-sub-sprint Codex content at S-Auto-5 close (PRIOR to archival at milestone close); milestone-shared Codex writes additional content at M-Auto-1B close; archived to `docs/milestones/M-Auto-1B_codex-review.md` at milestone close per standard deliver-agent close-out.
13. **No modification of `autoloop/autoloop/scoring/` baseline files**: `tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py` are locked against the M-Auto-1A close content hash `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`. Any change triggers `gaming.scoring_code_drift.sha_changed` ERROR per D3.
14. **No hardcoding of the Codex Axis B exact phrase** (or any specific eval case wording / user utterance / expected answer / case-status label) in any detector rule. Fix-C step 1 + optional Fix-B step 2 must encode generic structural shapes only (per D2 detector self-discipline). The detector self-discipline 3 regression tests (`test_detector_source_does_not_hardcode_eval_case_ids` + `..._user_utterance_literals` + `..._rule_count_bounded`) continue to PASS as the structural regression guard.
15. **No LLM call inside detector or content_validator** (per D1 detector regex-heuristic-only rule; M-Auto-1A baseline holds).
16. **No promotion of `gaming.observation_only_in_v1`** from `true` to `false` in M-Auto-1B (M-Auto-2+ decision; this milestone accumulates evidence only).
17. **At MOST 1 cherry-pick** during S-Auto-6 (fence #7 above is the exact rule; explicitly named here for redundancy).

### 6.1 OQ-S56.1 disposition (inherited from M-Auto-1A §6.1; unchanged)

The blessing for `eval_interactive/eval_interactive.yaml` env-var indirection (`bot.base_url: http://localhost:8080` → `${CSAGENT_BACKEND_URL}` + 7-line comment block) established at S-Auto-3 close 2026-05-27 / re-affirmed at M-Auto-1A close 2026-05-28 carries forward to M-Auto-1B unchanged. No second-edit surface; fence #2 still applies in full to the inner `eval_interactive/eval_interactive/**` module + case_specs + case_specs_shadow.

## 7. R-items consumed / surfaced

**Consumed by M-Auto-1B (closed at close)**:

- **`R-S57-anti-hardcode-whenever-arrow-synonym-bypass`** — S-Auto-5 implements Fix-C step 1 (word-boundary regex) + Fix-C step 2 (evidence-driven `synonym_map_enabled` toggle decision OR Fix-B FLAG rule fallback). M-Auto-1B close = end-to-end validation: detector now structurally closes the Codex Axis B exact bypass; deliver-agent annotates the close in `docs/action_bank.md` §5 + adds §6 close-action index row pointing at `docs/milestones/M-Auto-1B_objective.md` §12 closure verdict.

**Coupled (read-only awareness; not consumed):**

- `R-bad-case-parallel-session-establishment-flakiness` (M5-close priority-bumped) — S-Auto-6 overnight + close-day bad-case rerun use `parallel=1` per the recurrence evidence. The overnight batch accumulates further data points (each iter's bad_cases run is a session-create event); if recurrence frequency materially shifts during overnight, surface as observation toward root-cause investigation R-item.
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (semantic_planner) — natural overnight optimization target. Meta-agent may propose `discover_triage.procedure` / `critical_steps[].desc` edits to address it; if one such candidate becomes a cherry-pick candidate, the manual review at S-Auto-6 specifically validates the iwzx-case behaviour against the proposed edit's per-turn trace.
- `R-bad-case-suite-uc-ghij-seed-from-real-sessions` — NOT pre-seeded in M-Auto-1B. New cases (when seeded by a future independent track) would automatically enter auto-loop fitness via `bad_cases/` programmatic case_passed count. No M-Auto-1B action.
- `R-shadow-fixture-empty-form-session-create-400` (M5 NEW) — cs59s01 / cs59s02 deterministic HTTP-400 stays excluded from bot-side regression count per S-Auto-2 baseline_loader handling. M-Auto-1B does NOT change this handling.
- `R-eval-java-module-retirement` (M-Auto-2+ governance-hygiene) — NOT consumed; deferred.
- M5 carry-over projection-hygiene candidate (#4 C3 dedup + OQ-S52.4 `knowledge_hits` canonicalization) — independent milestone candidate; not consumed.

**Surfaced by M-Auto-1B (expected)**:

- Potential R-item: per-iteration elapsed-time observation if average overnight elapsed >40 min (would pressure M-Auto-2 optimization).
- Potential R-item: any new bypass surfaced by S-Auto-5 Codex per-sub-sprint review beyond R-S57 (would be downgrade-to-signal follow-up; M-Auto-2+ calibration target).
- Potential R-item: `shadow_disagreement_rate` first measurement (§6) — if rate is materially high, surface for M-Auto-2 investigation of meta-agent's shadow-blind proposing behaviour.
- Potential R-item: any gaming check ERROR-severity hits during overnight (observation-only_in_v1 records them; pattern accumulation drives M-Auto-2+ gating decision).
- Potential R-item: any new bad case discovered by deliver-agent + human during S-Auto-6 manual review of kept candidates (a manual-review FAIL on a previously-uncatalogued failure shape → new bad case → `bad_cases/_manifest.md` ledger entry per §5.6).
- Potential R-item: lessons_compactor LLM-distilled lesson surface — if K=10 lesson reveals a meta-prompt opportunity, surface for M-Auto-2+ planning.
- Potential R-item: `autoloop/results/report-m-auto-1b.html` (or successor) cross-run navigation gap (per §10.5 observability debt risk) — surface as M-Auto-2 R-item if needed.

**NOT consumed by M-Auto-1B (intentionally deferred)**:

- M3-B Single Handover Orchestrator P0 (`D-single-handover-orchestrator`) — release_gate.md §1.1 blocker; remains in candidate slate for milestone AFTER M-Auto-1B; deliver-agent + human re-evaluate at M-Auto-1B close.
- M5 carry-over projection-hygiene candidate — independent track.
- All other open R-items in `docs/action_bank.md` §5 unrelated to auto-evolution calibration or first cherry-pick.

## 8. Codex review plan (per §4.3)

**Default**: milestone-shared review at M-Auto-1B close. Single cumulative Codex pass over the commit range covering S-Auto-5 + S-Auto-6 + optional cherry-pick commit. Codex consumes:

- Both sub-sprint objectives + handoffs (`docs/sprints/sprint-058-{objective,handoff}.md` + `docs/sprints/sprint-059-{objective,handoff}.md`).
- This milestone objective (live during execution; archived to `docs/milestones/M-Auto-1B_objective.md` at close).
- The S-Auto-5 per-sub-sprint Codex archive (`compact/sprint-058-codex-review-prompt.md` and the corresponding section of the live `docs/codex-findings.md`; NOT re-litigated at milestone level).
- The cumulative commit range produced by both sub-sprints + cherry-pick (if any).
- The Python test baseline reproducibility check + S-Auto-5 anti-hardcode regression test coverage + S-Auto-6 overnight result artefacts.
- The bad-case suite manual review notes from the M-Auto-1B close run + shadow rerun result.

**Per-sub-sprint trigger**: S-Auto-5 invokes §4.3 trigger #2. The detector calibration change (Fix-C step 1 + step 2) touches the §1.7 structural guard; Codex must independently verify Fix-C has no design hole + the calibration evidence is sound BEFORE S-Auto-6 begins running it against real overnight propose outputs. Codex must return `pass / 0` (or `approve with downgrade-to-signal follow-up` if a residual borderline surfaces but is contract-expected per the S-Auto-5 prompt's calibration acceptance bar) BEFORE S-Auto-6 starts overnight.

S-Auto-6 is **conditionally per-sub-sprint** ONLY IF the cherry-pick candidate touches a §1.7 borderline that requires per-sub-sprint review (i.e., manual review finds a candidate at the "drift to keyword bot" edge case where programmatic PASS but manual judgment is split). Deliver-agent + human judge at the cherry-pick decision point; if per-sub-sprint Codex is triggered, it covers the S-Auto-6 commit range + cherry-pick commit BEFORE M-Auto-1B close.

**Verdict set** (§4.1): `approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.

### 8.1 M-Auto-1B-shared review prompt outline (drafted at close)

1. **Cumulative scope claim**: review commits `<M-Auto-1A-close>..<M-Auto-1B-close>` covering S-Auto-5 (detector calibration + 1-3 live-iter samples) through S-Auto-6 (overnight batch + cherry-pick if any) against this milestone objective.
2. **§4.1 nine-question kernel walk** (cumulative). Special attention to:
   - Q1 / Q5 / Q6: does the Fix-C step 1 word-boundary regex itself encode §1.7-violating decision logic? (The defender must remain generic structural per D2.) Does the optional Fix-B FLAG rule (if step 2 chose fallback) similarly remain generic structural?
   - Q2: does S-Auto-5 or S-Auto-6 attempt to create any new Tier-0 invariant? (None expected; verify.)
   - Q4: does the cherry-picked Skill YAML edit (if any) introduce eval case_id / session_id / known eval phrase references? (Sandbox + anti-hardcode auto-check should have rejected such edits at propose-stage; verify at cumulative level.)
   - Q6 / Q9: does the cherry-pick candidate manual review respect the §5.6 human-judgment gate? Does the AskUserQuestion decision record carry the deliver-agent + human joint justification?
3. **§1.7 boundary check** on the Fix-C step 1 regex + optional Fix-B FLAG regex + any cherry-picked Skill YAML edit + any test fixture added during S-Auto-5 calibration.
4. **§4.2 sprint-close header** filled (`pass | fix_required | out_of_scope_review` + `blocking_count` + summary).
5. **Hard-fence verification** against §6 (17 items) via `git diff <M-Auto-1A-close>..HEAD --stat` and targeted spot-checks (specifically: scoring code SHA unchanged → `gaming._check_scoring_code_drift` returns empty; Skill YAML diff if any is exactly ONE file + ONE field-class).
6. **Reproducibility checks** per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every numeric claim in the deliver-agent's M-Auto-1B close package must be reproducible (calibration FLAG rate, overnight kept/discard counts, baseline envelope IQR, per-iteration elapsed-time average, lessons_compactor lesson count).
7. **Cherry-pick decision audit**: if cherry-pick landed, Codex verifies (a) the candidate passed programmatic verdict (Layer 0-4 PASS); (b) deliver-agent + human manual review recorded PASS in handoff §X; (c) AskUserQuestion record exists; (d) `python -m autoloop apply --experiment exp-<N>` Hybrid path was followed; (e) human manual commit signature is present; (f) `config.fitness.baseline_dir` advanced accordingly.
8. **Anti-hardcode bypass spot-check (cumulative)**: Codex constructs ≥3 NEW adversarial propose outputs (beyond R-S57 Axis B; e.g., German / Spanish translation of "whenever ... =>"; LaTeX-like notation "$\Rightarrow$" arrow variant; logically-equivalent multi-line "Given X happens, then output Y"); confirms detector rejects each correctly OR records as fresh `approve with downgrade-to-signal follow-up` if a residual surfaces (would be a NEW R-item for M-Auto-2+).
9. **Bad-case + shadow human-judgment-gate respect**: Codex MUST NOT auto-PASS / auto-FAIL the bad-case manual review or shadow rerun verdict; reads deliver-agent + human notes and verifies internal consistency.
10. **Deferred / non-blocking notes**: observations for M-Auto-2+ planning (Stage-2 entry signal, M3-B re-evaluation signal, projection-hygiene candidate signal).

## 9. Estimated milestone duration

**Calendar estimate (informational; not a gate):**

- S-Auto-5 / Sprint 058: 4-5 dev-days (1 day prereqs + 1 day live-iter + 1-2 days Fix-C step 1 implementation + calibration evidence + 1 day Codex coordination) + per-sub-sprint Codex ~2 days.
- S-Auto-6 / Sprint 059: 1-2 dev-days execution (pre-batch baseline + overnight kick + monitoring) + 1 overnight (6-8h auto-loop) + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply.
- M-Auto-1B close: deliver-agent + human bad-case manual review + shadow rerun + milestone-shared Codex review + close-out artefacts ~2-3 days.

**Total**: ~2.5-3 calendar weeks for the full milestone, assuming sub-sprints execute sequentially, S-Auto-5 Codex returns `pass` on first pass, and no overnight halt-trigger fires.

Risk to duration:
- S-Auto-5 Codex `reject` triggers fix-iteration sub-sprint (S-Auto-5.1) — extends ~1 week. Mitigation: deliver-agent pre-walks the Fix-C step 1 regex against the existing 17-fixture sweep + ≥10 real-meta-agent samples BEFORE dev commit; dev runs the detector self-discipline regression test + adversarial spot-checks before commit.
- Overnight infrastructure halt (LLM API ban, mvn cache miss, Spring restart pathology) — could lose 1 overnight window; mitigation: S-Auto-5 first live-iter measures latency + error rate, deliver-agent + human pre-evaluate overnight readiness.
- First cherry-pick candidate manual review surfaces borderline §5.3 case requiring discussion — could extend manual review by 1-2 days; mitigation: deliver-agent prepares §5.3 decision rubric ahead of human review session.

## 10. Stop conditions (milestone-level)

**Stop signals (deliver-agent + human reassess scope; possibly invoke in-flight downgrade):**

1. S-Auto-5 live-iter prerequisites cannot be satisfied within 1 dev-day (e.g., `mvn package` repeatedly fails due to dependency issues, disk space, or environment misconfiguration). Halt; surface to deliver-agent + human; investigate build infrastructure (NOT bypass with skip-tests-blanket / dependency-pinning hacks).
2. S-Auto-5 first live iteration crashes inside the 14-step state machine in a way the S-Auto-3 crash-recovery substrate does NOT handle (e.g., a path-not-tested-during-S-Auto-3-integration-test). Halt; root-cause investigation; possibly fix-iteration before continuing.
3. S-Auto-5 real-meta-agent calibration FLAG rate ≥25% (warn threshold) AND the violations are NOT all attributable to a single rule needing narrowing. Halt; detector is too noisy on real distribution; re-design rules (NOT widen FLAG threshold).
4. S-Auto-5 Codex per-sub-sprint review returns `reject as semantic hardcode` on Fix-C step 1 OR step 2 fallback rule. Halt; the detector itself violates §1.7 — re-design with deliver-agent + human + research-agent.
5. S-Auto-6 pre-batch baseline drift envelope >10/34 cases (~30%; 2× the M-Auto-1A close-day signature). Halt; upstream LLM-provider behaviour shifted materially; pause overnight + investigate.
6. S-Auto-6 overnight halts before iteration 5 (e.g., total errors >50% within the first 5 iters). Halt; LLM API + infrastructure investigation; possibly recover the iters already completed and re-run; deliver-agent + human decide.
7. S-Auto-6 overnight produces ZERO kept candidates AND deliver-agent + human jointly judge this is a meta-agent prompt issue (not just bad luck on the propose distribution). Halt; surface as M-Auto-2 meta-prompt refinement R-item; M-Auto-1B close PASS still possible IF all OTHER gates pass.
8. M-Auto-1B close-time bad-case manual review surfaces a NEW failure shape (a previously-PASSing case now FAILing) NOT explained by either the cherry-pick edit OR LLM-provider drift. Halt; investigate (auto-loop has somehow side-effected the bot via an unexpected path); possibly revert cherry-pick + re-investigate.
9. M-Auto-1B close-time shadow rerun surfaces ANY NEW regression beyond `R-shadow-fixture-empty-form-session-create-400` AND beyond cherry-pick-attributable drift ≤3%. Halt; investigate; revert cherry-pick if necessary.

**Continue signals (do NOT halt):**

- Per-iteration elapsed time observation: >25 min but <40 min — record as observation, plan for M-Auto-2 optimization.
- S-Auto-5 calibration produces 1-2 FLAG_FOR_CODEX cases on real-meta-agent samples — record per-sample evidence in handoff; manual review at S-Auto-5 close; deliver-agent + human decide whether to narrow rule or accept as `approve with downgrade-to-signal follow-up`.
- S-Auto-6 overnight produces 0 kept candidates DUE TO Tier-0 / Tier-1 / Tier-2 / shadow-drop discards (the substrate working as designed). M-Auto-1B close PASS is possible with 0 cherry-pick + explicit "no human-approved candidate" justification, IF all other gates pass.
- LLM-provider drift in close-day bad-case / shadow rerun matching the M-Auto-1A close-day signature (5/34 one-direction `True/0.5 → False/0.0`) — observation only; bot byte-identical-or-cherry-pick-attributable check distinguishes drift from regression.
- Smoke composite_score moves due to LLM provider drift — per §5.5, observation only; not a stop signal.

## 11. Cross-milestone sequencing context

**Prior milestones**:

- **M-Auto-1A (Auto-Evolution Build)** — Clean PASS 2026-05-28. Substrate complete; M-Auto-1B is the first real workout. The OQ-S56.5 waiver (live-iter deferred) is retired at S-Auto-5 acceptance bar. The detector calibration follow-up (`R-S57-anti-hardcode-whenever-arrow-synonym-bypass`) is M-Auto-1B's primary R-item.
- **M5 (Observability Coherence)** — Clean PASS 2026-05-25. Provides the four-tier `report.html` + per-invocation admin trace surfaces M-Auto-1B's human review uses (S-Auto-6 opens admin trace for per-turn LLM raw response inspection of kept candidates).
- **M4-Eval-Cleanup, M3-Eval, M2, M1** — preserved baselines (smoke demoted to observation; four-tier evaluation framework; Skill registry; DISCOVER + Intake). M-Auto-1B does not touch any of these.

**Next milestones (post-M-Auto-1B)**:

- **Stage-2 entry decision** — separate milestone determination after M-Auto-1B close. Evaluated against M-Auto-1B's evidence: ≥3 kept human-approved cherry-picks + 0 borderline §5.3 cases would be a positive signal. M-Auto-1B itself does NOT pre-commit Stage-2 entry criteria.
- **M3-B Single Handover Orchestrator (P0)** — release_gate.md §1.1 blocker; remains in candidate slate; deliver-agent + human re-evaluate at M-Auto-1B close.
- **Projection-hygiene milestone candidate** (M5 carry-over #4 + OQ-S52.4) — independent track; deliver-agent + human reassess at M-Auto-1B close.
- **UC-G/H/I/J bad-case seeding, semantic-planner soft-signal extension, M-Auto-2 (extended optimization / overnight scale-up), `R-eval-java-module-retirement`, Latency / Skill-Tuning / Tier-0 re-evaluation** — remain in candidate slate; M-Auto-1B's overnight evidence may reshuffle priorities.

## 12. Closure verdict

*Filled by deliver-agent + human at M-Auto-1B close per `iteration_governance.md` §8.4.*

**Status as of authoring (2026-05-28)**: not started; planning round complete; S-Auto-5 dev session ready to start once human approves objective + sprint contract + dev prompt.

Per the §8.4 close-out artefacts list, at M-Auto-1B close the deliver-agent + human will:

1. Update `docs/10-handoff.md` §0 + §1 + §2 with the M-Auto-1B close lead + archive index row.
2. `git mv docs/milestone_objective.md → docs/milestones/M-Auto-1B_objective.md`.
3. `git mv docs/codex-findings.md → docs/milestones/M-Auto-1B_codex-review.md`; reset live `docs/codex-findings.md` to scaffold.
4. Reset live `docs/milestone_objective.md` + `docs/sprint_objective.md` to next-milestone-TBD placeholders.
5. Update `docs/action_bank.md` §6.5 closed-milestone-index row + R-S57 close annotation in §5.
6. Fill in this §12 with: classification (A — Clean / A-with-OOSR / B — Fix iteration / ...), cumulative scope (commit range), per-sub-sprint dispositions table, close gate audit (15+ items above), R-item flips (R-S57 close), architecture-health metric direction, cross-milestone observations, next planning round candidate, reproducibility check.
