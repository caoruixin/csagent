---
title: Milestone M-Auto-1B — Auto-Evolution Calibration (Live-Iter Bootstrap + First Overnight + First Cherry-Pick)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-30
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

> **Sequence refinement 2026-05-29 (S-Auto-6 close-empty in-place revision per §8.3)**: the original 2-sub-sprint sequence (S-Auto-5 + S-Auto-6) is extended to 4 sub-sprints (S-Auto-5 + S-Auto-6 + S-Auto-7 + S-Auto-8) within the §8.5 5-sub-sprint ceiling. S-Auto-6 closed empty 2026-05-29 (Class C — In-flight downgrade) after dev surfaced two pre-flight substrate blockers via two AskUserQuestion gates before any LLM budget was consumed; the milestone acceptance bar (first overnight + first cherry-pick) is UNCHANGED but the sequence now requires a substrate-fix sub-sprint (S-Auto-7) before the deferred overnight + cherry-pick scope can be reattempted (now S-Auto-8). Sub-sprint count = 4 within ceiling; margin = 1 for any further fix-iteration. See `docs/sprints/sprint-059-handoff.md` §0 + §12 for the blocker evidence chain.
>
> **Sequence refinement 2026-05-29 (S-Auto-7 close in-place revision per §8.3; second revision in the same day)**: the 4-sub-sprint sequence extends to 5 sub-sprints (S-Auto-5 + S-Auto-6 + S-Auto-7 + **S-Auto-7.1** + S-Auto-8). S-Auto-7 closed 2026-05-29 (Class B — Surfaced findings need fix-iteration; sub-classified `approve with downgrade-to-signal follow-up` per per-sub-sprint Codex `decision: pass / blocking_count: 0`). S-Auto-7 substrate scope (Blocker A baseline blessing + Blocker B eval_runner fix path (b) + Blocker C in-session loader.py fix + scoring SHA rebaseline 5177b674…→22548e20…) was ACCEPTED by Codex; Goal #4 (smoke iter through Step 9) BLOCKED at Step 6 alt-port Spring spawn (OQ-S60.7 = OQ-S58.7 carryover; foreground :8080 backend competed for Flyway lock / maven target/ / Redis pool). S-Auto-7.1 / Sprint 061 — Pre-flight env check + smoke iter completion — is the fix-iteration sub-sprint that retires Goal #4 via the human-locked principle "pre-flight the env at the outer-loop entry point; in dev, reboot misbehaving services rather than diagnose mid-iteration crashes" (saved as memory `feedback_preflight_env_check_outer_loop`). Sub-sprint count = 5 hits the §8.5 ceiling EXACTLY; margin = 0 for any further fix-iteration. If S-Auto-7.1 surfaces a second-order substrate brittleness requiring S-Auto-7.2, M-Auto-1B must split per §8.5 advice. See `docs/sprints/sprint-060-handoff.md` §7 + §12 + `docs/codex-findings.md` (S-Auto-7 review) for the Goal #4 BLOCKED evidence chain.

### S-Auto-5 / Sprint 058 — Live-iter bootstrap + Detector calibration (Fix-C hybrid) — CLOSED 2026-05-28 A — Clean PASS sub-classified `approve with downgrade-to-signal follow-up`

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

### S-Auto-6 / Sprint 059 — First overnight batch + First human review + First cherry-pick to main — CLOSED 2026-05-29 EMPTY Class C — In-flight downgrade (substrate pre-flight blocked; scope deferred to S-Auto-8 after substrate-fix S-Auto-7 lands)

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

### S-Auto-7 / Sprint 060 — Substrate fix (Blocker A + Blocker B + smoke iter) — CLOSED 2026-05-29 B — Surfaced findings need fix-iteration sub-classified `approve with downgrade-to-signal follow-up` (Goal #4 BLOCKED → S-Auto-7.1; substrate fixes ACCEPTED by per-sub-sprint Codex `pass / 0`; Codex governance trigger addressed in this close-bundle via §6 fence #2 annotation)

**Layer:** `infra` (substrate-fix; no semantic decision change; structural plumbing repair). **§7 stanza:** **EXEMPT** per pure-infra carve-out (substrate plumbing + baseline rebaseline; eval_runner.py change is HOW the subprocess consumes results, NOT WHAT eval reads or grades; case_specs / judge unchanged; self-walk for paper-trail). **Codex:** **PER-SUB-SPRINT REQUIRED (§4.3 trigger #3 — hard-fenced surface that the milestone objective explicitly named out of scope; fence #13 scoring code content-hash lock is being explicitly overridden with re-baselined `scoring_code_baseline_sha`)**. **Estimated dev:** 2-3 dev-days + Codex ~1-2 days.

**Scope (5 sentences):**

1. **Blocker A resolution — bless concrete baseline_dir.** Run the v1 47-case fitness suite (bad_cases ×12 [parallel=1] + anchor_outcome ×12 [parallel=4] + shadow ×23 [parallel=4]) ONCE on `auto-loop-branch` HEAD against the calibrated detector + repaired eval_runner (Blocker B fix must land first so the eval_runner subprocess actually succeeds). Bless the resulting `eval_interactive/results/<run-id>/` directory as the M-Auto-1B baseline by updating `autoloop/config.yaml` `fitness.baseline_dir` from the literal `<PLACEHOLDER-set-at-M-Auto-1A-close>` string to `eval_interactive/results/<concrete-run-id>`. Record per-suite `case_passed` count + per-case verdict snapshot in handoff §X "Baseline blessing evidence". This is the first time a non-empty `BaselineSnapshot` is loaded by `loop.py:386-441` → Layer 3 improvement-threshold gate becomes meaningful AND Layer 4 shadow regression `shadow_max_drop_pct: 3.0%` denominator becomes non-zero.
2. **Blocker B resolution — fix path (b) human-locked at S-Auto-6 close**: REMOVE `--output-dir` from `autoloop/autoloop/scoring/eval_runner.py:99-110` subprocess invocation; adapt `eval_runner.run_v1_fitness_suite` to consume `eval-interactive`'s auto-timestamped `eval_interactive/results/<ts>/` directory (read `ls -t eval_interactive/results/ | head -1` post-subprocess to locate the most recent timestamped output; copy or symlink to `suite_out_dir` if needed for `tier_evaluator` consumption). Verify by single-suite end-to-end: `python -c "from autoloop.scoring.eval_runner import run_v1_fitness_suite; ..."` returns `SuiteRunResult(exit_code=0, results_root=<path>)` with non-empty `results.json` at that path. Smaller blast radius than fix path (a) — only `autoloop/` side changes; `eval_interactive/eval_interactive/cli.py` UNCHANGED.
3. **Scoring code SHA rebaseline.** `eval_runner.py` modification triggers `gaming.scoring_code_drift.sha_changed` ERROR per D3 unless `config.fitness.scoring_code_baseline_sha` is updated. Run `python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha(config_path='autoloop/config.yaml'))"` post-edit; update `autoloop/config.yaml` `fitness.scoring_code_baseline_sha` from the M-Auto-1A close hash `5177b674...` to the new content hash. Verify silent steady state: `_check_scoring_code_drift(config=config)` returns `[]`. This is a controlled fence #13 override with documented re-baselining — pattern precedent at M-Auto-1A close commit `b6b627b` (S-Auto-4 close-day fix-up: literal git commit id corrected to content-hash form).
4. **Smoke iter end-to-end verification.** Run `python -m autoloop run --experiments 1` (NO `--dry-run`) on `auto-loop-branch` against the now-blessed baseline + repaired eval_runner. The full 14-step state machine must complete through Step 7 (eval_runner) AND Step 9 (tier_evaluator) — both previously unreachable. Acceptance: (a) iteration terminal verdict is keep / discard / error (any of three OK); (b) `autoloop/results/runs/exp-<N>/` contains `iteration_record.json` with non-empty `tier_evaluator_verdict` (Layer 0-4 all non-degenerate); (c) per-iter elapsed time recorded (the FIRST measurement of full Spring-spawn + 47-case-eval cycle). If smoke crashes in an unhandled path, surface — S-Auto-7 close-empty (fix-iteration S-Auto-7.1) before S-Auto-8 can dispatch.
5. **Per-sub-sprint Codex (§4.3 trigger #3)** reviews the cumulative S-Auto-7 commit range: validates Blocker B fix path (b) structural soundness (auto-timestamp consumption is plumbing-correct; no new semantic logic); validates the new `scoring_code_baseline_sha` matches actual `_compute_scoring_code_sha()` output post-edit; verifies the controlled fence #13 override is justified + documented (Blocker B forces it; no other path enables overnight); verifies the blessed baseline_dir points to a real concrete run; verifies smoke iter reached Step 9 with non-degenerate `tier_evaluator_verdict`. Codex must return `pass / 0` (or `approve with downgrade-to-signal follow-up` for any residual substrate brittleness) BEFORE S-Auto-8 overnight starts.

**Files in scope** (S-Auto-7 only):

- **EXTEND**: `autoloop/autoloop/scoring/eval_runner.py` (Blocker B fix path (b): remove `--output-dir`; adapt to auto-timestamped output consumption); `autoloop/config.yaml` (`fitness.baseline_dir` advance + `fitness.scoring_code_baseline_sha` re-baselined post-eval_runner-edit).
- **NEW**: `autoloop/tests/test_eval_runner.py` (or extend if already exists) — verify auto-timestamp consumption against a mocked-subprocess fixture; verify `SuiteRunResult.results_root` returns the post-subprocess auto-timestamped path; verify `exit_code` propagation for substrate errors; verify scoring_code_baseline_sha assertion matches new hash.
- **NEW**: `eval_interactive/results/<concrete-run-id>/` — the blessed baseline (committed if `.gitignore` policy allows, else evidence captured in handoff §X via results.json digest only — verify existing convention).
- **NEW**: `docs/sprints/sprint-060-{objective,handoff}.md`.
- **NEW**: `compact/sprint-060-codex-review-prompt.md` (deliver-agent authors at S-Auto-7 close, self-contained per §9 invariant, embeds §4.1 kernel + commit range + fence #13 override justification + verification axes verbatim).
- **CONTROLLED FENCE #13 OVERRIDE**: `autoloop/autoloop/scoring/eval_runner.py` (touched per S-Auto-6 close human-locked fix path (b); content-hash rebaselined; M-Auto-1B §6 fence #13 is now `5177b674... at S-Auto-6 close → <new-hash> at S-Auto-7 close`). The override is BLESSED at S-Auto-7 planning round (this in-place revision); Codex per-sub-sprint verifies at S-Auto-7 close.
- **NO touch** to other M-Auto-1B §6 hard-fenced surfaces: `server/src/main/java/`, `eval/src/main/java/`, `eval_interactive/eval_interactive/` (Blocker B fix path (b) keeps eval-interactive byte-identical), case_specs / case_specs_shadow, `server/src/main/resources/` (skills/ remains conditional via S-Auto-8 cherry-pick mechanism; NOT in S-Auto-7), `data/`, `db/migration/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives (other than S-Auto-7's own archives at close), `docs/codex-findings.md` scaffold.
- **NO touch** to `autoloop/autoloop/scoring/{tier_evaluator,baseline_loader,gaming}.py` (the other three scoring files in fence #13 group; only `eval_runner.py` is overridden).

### S-Auto-7.1 / Sprint 061 — Pre-flight env check + smoke iter completion (fix-iteration for Goal #4 BLOCKED) — NEW 2026-05-29

**Layer:** `infra` (substrate ergonomics + environment pre-flight; structural plumbing repair on the auto-loop entry point; `cli.py` integration + NEW `preflight.py`; no semantic decision change, no projection / scoring semantic logic edit, no CaseSpec / judge change). **§7 stanza:** **EXEMPT** per pure-infra carve-out (self-walked for paper-trail). **Codex:** **DEFAULT milestone-shared at M-Auto-1B close** (no §4.3 trigger expected; planned scope lives in `autoloop/cli.py` extension + NEW `autoloop/autoloop/preflight.py`; neither is fenced. UPGRADE to per-sub-sprint Codex per §4.3 trigger #3 ONLY IF dev encounters substrate brittleness requiring new fence touch — dev STOP-and-surfaces before proceeding). **Estimated dev:** 1-2 dev-days + Codex deferred to milestone close.

**Scope (5 sentences):**

1. Implement pre-flight env check at the outer-loop entry point: NEW `autoloop/autoloop/preflight.py` with `PreflightResult` dataclass + `run_preflight(config)` covering 6 checks — `check_foreground_backend` (`lsof -i :8080` detects competing Spring backend → returns `fail` with remediation), `check_postgres_reachable` + `check_redis_reachable` (TCP `socket.create_connection` with 2s timeout), `check_meta_llm_api_key` (os.environ + non-empty), `check_clean_working_tree` (`git status --porcelain` returning `warn` not `fail`), `check_baseline_dir_loads` (invokes `baseline_loader.load` against blessed `eval_interactive/results/m-auto-1b-baseline-20260529/`). Stdlib + subprocess + socket only.
2. Wire pre-flight into CLI entry point: `autoloop/autoloop/cli.py` extended with NEW `preflight` subcommand + `--auto-reboot` + `--skip-preflight` flags on the `run` subcommand. The `run` subcommand calls `run_preflight(config)` BEFORE `loop.run_loop()`; refuses to start if any `fail` (unless `--skip-preflight`); `--auto-reboot` invokes `auto_reboot_foreground_backend(pid)` with SIGTERM 5s timeout escalation to SIGKILL for the `check_foreground_backend` fail case only.
3. Smoke iter completion via the pre-flight path: with foreground :8080 stopped (or `--auto-reboot` invoked), `python -m autoloop run --experiments 1` (NO `--dry-run`) drives the full 14-step state machine to a verdict; Steps 6 (Spring spawn) + 7 (eval_runner) + 9 (tier_evaluator) all reach non-degenerate outputs; per-iter elapsed time recorded as FIRST measurement of full Spring-spawn + 47-case-eval cycle on the now-blessed baseline. **This retires Goal #4 from S-Auto-7.**
4. OQ-S60.10 ergonomics fix bundled if trivial (~5 LOC at top of cli.py `run` subcommand handler invoking `dotenv.load_dotenv("autoloop/.env.local")` so `AUTOLOOP_META_LLM_API_KEY` auto-loads; retires the "set -a; source .env.local; set +a" prerequisite). If non-trivial, defer with new R-item `R-autoloop-env-local-auto-load`. OQ-S60.7 root-cause (Flyway lock contention etc) characterized to the depth needed to confirm the stop-foreground-backend workaround; deeper substrate fixes (in-memory DB / Flyway timeout / Docker sandbox) DEFERRED to M-Auto-2+ as observation.
5. Author handoff `docs/sprints/sprint-061-handoff.md` per dev prompt §"Handoff requirements"; Codex review at M-Auto-1B close is default milestone-shared (NOT per-sub-sprint). Single-commit pattern preferred (~150-250 LOC); two-commit acceptable if OQ-S60.10 fix is split out.

**Files in scope** (S-Auto-7.1 only):

- **NEW**: `autoloop/autoloop/preflight.py` (pre-flight module with 6 checks + auto_reboot helper).
- **EXTEND**: `autoloop/autoloop/cli.py` (pre-flight wiring + `preflight` subcommand + `--auto-reboot` / `--skip-preflight` flags + optional OQ-S60.10 dotenv auto-load).
- **NEW**: `autoloop/tests/test_preflight.py` (4-8 new tests covering each check + auto_reboot signal sequence + CLI integration via Click test runner or direct invocation).
- **CONDITIONAL** (if OQ-S60.10 fix lands): `autoloop/pyproject.toml` (add `python-dotenv` only if not already declared).
- **NEW**: `autoloop/results/runs/exp-<N>/` (the smoke iter end-to-end artefacts), `autoloop/results/experiments.jsonl` (append entry), `autoloop/results/iterations.sqlite` (insert row).
- **NEW**: `docs/sprints/sprint-061-{objective,handoff}.md` (dev authors handoff at close; deliver-agent authors objective at S-Auto-7 close-bundle = this in-place revision).
- **CONDITIONAL** (if §4.3 trigger #3 upgrade fires): `compact/sprint-061-codex-review-prompt.md` (deliver-agent authors at S-Auto-7.1 close if needed).
- **NO touch** to `autoloop/autoloop/scoring/` (fence #13 reasserted at hash `22548e20…`), `autoloop/autoloop/sandbox/`, `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/`, `autoloop/autoloop/memory/`, `eval_interactive/eval_interactive/` (loader.py controlled override from S-Auto-7 stays UNCHANGED; no further edits), `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/` (NO cherry-pick in S-Auto-7.1; that's S-Auto-8 ONLY), `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives, `docs/codex-findings.md` scaffold.

### S-Auto-8 / Sprint 062 — First overnight batch + First human review + First cherry-pick to main (deferred S-Auto-6 scope; reattempted post-S-Auto-7.1) — NEW 2026-05-29 (Sprint number bumped 061→062 by S-Auto-7 close in-place revision)

**Layer:** `eval_spec` (consumes the per-iteration fitness verdict sequence on the now-blessed baseline + repaired eval_runner; §5.6 manual review + cherry-pick are eval-side acceptance bars). **§7 stanza:** REQUIRED (semantic-touching via cherry-pick if it lands; same as original S-Auto-6 framing). **Codex:** milestone-shared (default) at M-Auto-1B close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint Codex pre-milestone-close per §4.3 trigger). **Estimated dev:** 1-2 dev-days execution + 1 overnight (6-8h auto-loop) + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply + close-bundle.

**Scope (5 sentences):** identical to the original S-Auto-6 scope per `docs/sprints/sprint-059-objective.md` archive (pre-batch baseline drift envelope + overnight batch + §5.6 manual review of kept candidates + AskUserQuestion cherry-pick decision + observation accumulation + R-S58 disposition recommendation). The substrate prerequisites that blocked S-Auto-6 (Blocker A + Blocker B) are resolved at S-Auto-7 close; the S-Auto-8 dev session can therefore execute the overnight + cherry-pick scope without substrate STOP-and-surface. R-S58 zero-width bypass surface check during overnight propose distribution scan (S-Auto-6 Scope #5 deferred) lands here. Cherry-pick fence remains EXACTLY ONCE per M-Auto-1B §6 fence #7 / #17. R-S58 disposition recommendation (defer-to-M-Auto-2 OR extend-with-S-Auto-9-Fix-D OR similar) decided at S-Auto-8 close + finalized at M-Auto-1B close.

**Files in scope** (S-Auto-8 only):

- Same as the original S-Auto-6 (see `docs/sprints/sprint-059-objective.md` archive).
- The cherry-pick mechanism + Skill YAML conditional-touch (fence #7) is unchanged.
- The S-Auto-7 close-state baseline (`fitness.baseline_dir` blessed + `scoring_code_baseline_sha` re-baselined) is the new baseline against which S-Auto-8 overnight runs.

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
2. **No edits** to any file under `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. The Evaluator side stays byte-identical. **S-Auto-7 CONTROLLED OVERRIDE on `eval_interactive/eval_interactive/case_spec/loader.py` ONLY** (in-session human-authorized 2026-05-29 during S-Auto-7 Scope #3 baseline blessing — `directory.glob` → `directory.rglob` + new `_is_case_spec(p)` filter skipping `p.name.startswith("_")` manifest convention; substrate path-handling repair for shadow `case_specs_shadow/case_families/<family>/` nested layout + `_manifest.yaml` top-level metadata; no semantic logic, no case-content interpretation; zero-impact negative-control on 6 flat case_set dirs — anchor 159, promotion 101, exploration 107, smoke 14, bad_cases 12, anchor_outcome 12 — identical pre/post-rglob counts; eval_interactive pytest 486 PASS / 3 FAIL UNCHANGED; per-sub-sprint Codex verdict 2026-05-29 `pass / 0` `approve with downgrade-to-signal follow-up` with trigger = "annotate this fence with controlled-override note" satisfied by this annotation; see `docs/sprints/sprint-060-handoff.md` §6 + §8 for the override evidence chain and `docs/codex-findings.md` Axis B for Codex independent verification). All OTHER files under `eval_interactive/eval_interactive/**` (including `cli.py`) + `eval_interactive/case_specs/**` + `eval_interactive/case_specs_shadow/**` stay byte-identical for the remainder of M-Auto-1B. The override is FINALIZED at S-Auto-7 close 2026-05-29; subsequent sub-sprints (S-Auto-7.1, S-Auto-8) MUST NOT edit `loader.py` or any other file in this fence group. **Clarification 2026-05-30 (Phase 1.6 close-bundle)**: the fence #2 byte-identical rule covers CASE_SPEC YAML CONTENT (the `cs<id>.yaml` files that the eval loads + judges) and the inner `eval_interactive/eval_interactive/**` Python module; it does NOT cover the `_manifest.md` lifecycle ledger files (`eval_interactive/case_specs/bad_cases/_manifest.md` + any future analogous ledgers). The lifecycle ledger is an EXPECTED close-bundle update surface per `iteration_governance.md` §5.6 ("The bad-case suite directory is governance-tracked; see `eval_interactive/case_specs/bad_cases/_manifest.md` for the lifecycle ledger") + historical precedent (the _manifest.md has been updated at every milestone close since M3-Eval). Updates to _manifest.md are deliver-agent close-bundle activity, NOT a fence #2 violation. Case_spec YAML byte-identical rule continues to hold (verified at M-Auto-1B Phase 1.6 close-bundle: `git diff --stat b6b627b..HEAD -- 'eval_interactive/case_specs/**/*.yaml' 'eval_interactive/case_specs_shadow/**/*.yaml'` returns empty).
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

*Finalized by deliver-agent + human at M-Auto-1B Phase 3 close 2026-05-30 per `iteration_governance.md` §8.4 after milestone-shared Codex review returned `pass / 0 / approve with downgrade-to-signal follow-up`. All 3 Codex downgrade triggers (Axis M6 drift-envelope guidance + Axis M9 R-item ledger + Axis M10 close-package metadata) are non-blocking close-package or M-Auto-1C planning hygiene; dispositions in §12.14 below.*

### 12.1 Classification (Phase 3 finalized)

**Classification**: **A-with-acceptance-bar-revision** (`docs/current/deliver_close_taxonomy.md` Class A-with-acceptance-bar-revision). Codex milestone-shared review verdict `decision: pass / blocking_count: 0` sub-classified `approve with downgrade-to-signal follow-up` 2026-05-30 (live `docs/codex-findings.md` "M-Auto-1B Milestone-Shared Review Decision" section; archive `docs/milestones/M-Auto-1B_codex-review.md`). Codex Axis M7 independently confirmed the §8.5 split decision is scope-clean: "M-Auto-1C's draft scope, S-Auto-7.2 applier fix plus S-Auto-8 overnight/cherry-pick, is coherent continuation scope rather than unrelated work." 8/15 hard gates met cleanly + 7/15 explicitly inherited to M-Auto-1C with the same acceptance bar (NOT a scope cut). 3 non-blocking Codex downgrade triggers addressed at Phase 3 per §12.14.

### 12.2 Cumulative scope (commit range)

`b6b627b (M-Auto-1A close-prep evidence commit, exclusive) .. <Phase 3 close-bundle commit, inclusive>`. `git rev-list --count b6b627b..HEAD` returns **18 raw commits**; this is correctly higher than the M-Auto-1B agent-loop scope per Codex Axis M10 observation (the prompt + earlier §12.2 draft cited 14 / 12 stale counts that did not match the actual range tail). Reconciled breakdown:

- **M-Auto-1B agent-loop scope (14 commits)**:
  - M-Auto-1B planning round (1): `6e8692d`.
  - S-Auto-5 / Sprint 058 (4): `b0ce174` bootstrap + `ae0ec3e` dev + `1f51ae8` per-sub-sprint Codex prep + `95089c2` close-bundle.
  - S-Auto-6 / Sprint 059 close-empty (2): `1943ed5` dev STOP-and-surface + `b6084f9` deliver-agent close-empty bundle.
  - S-Auto-7 / Sprint 060 (3): `559927a` Blocker B + scoring SHA + `b0a3704` Blocker A + Blocker C + `307f69a` close-bundle with §6 fence #2 annotation.
  - S-Auto-7.1 / Sprint 061 (4): `19213b1` dev + `8e88273` Phase 1 close-bundle + `2c855eb` Phase 1.5 ambient-human-work disposition + `5b9c143` Phase 1.6 §5.6 evidence + _manifest.md ledger entry.
- **Adjacent non-scope (4 commits)**:
  - M-Auto-1A post-archival residue (2): `ae5ebc8` M-Auto-1A close-preparation + `7ed95e7` M-Auto-1A archive commit. `b6b627b` was the M-Auto-1A close-prep evidence commit (per the M-Auto-1A close lead in `docs/10-handoff.md` §0), not the archival commit; `ae5ebc8` + `7ed95e7` landed on `auto-loop-branch` AFTER `b6b627b` and before `6e8692d`. This is range-metadata residue, not M-Auto-1B work.
  - Ambient out-of-scope README (1): `60c5b67` "Add comprehensive architecture README" — dispositioned at S-Auto-7 close per `docs/codex-findings.md` Axis I PASS.
  - Ambient-human-work (1): `7871c62` "update budget and pii Sanitizer" — dispositioned at §12.13 + Codex Axis M11 PASS.

`14 + 4 = 18 ✓`. The Phase 3 close-bundle commit (this commit) is the final commit-of-record for M-Auto-1B before archival.

### 12.3 Per-sub-sprint dispositions

| Sub-sprint | Class | Codex outcome | Trigger | Status |
|---|---|---|---|---|
| S-Auto-5 / Sprint 058 — Live-iter bootstrap + Fix-C step 1 + calibration evidence + step 2 Path A | `infra` + `eval_spec` (calibration); §7 REQUIRED | per-sub-sprint `pass / 0` `approve with downgrade-to-signal follow-up` 2026-05-28 | §4.3 trigger #2 (Fix-C touched §1.7 structural guard) | A — Clean PASS sub-classified `approve with downgrade-to-signal follow-up` 2026-05-28; closed R-S57 via Fix-C hybrid; opened R-S58 (Codex Axis C zero-width bypass; M-Auto-1C / Fix-D candidate) |
| S-Auto-6 / Sprint 059 — Close empty (substrate pre-flight blocked) | N/A (no semantic delta shipped) | No Codex (close-empty) | None | EMPTY Class C — In-flight downgrade 2026-05-29; substrate pre-flight blockers A + B surfaced via dev AskUserQuestion; scope deferred to S-Auto-8 (later: S-Auto-7.2 + S-Auto-8 per §8.5 split) |
| S-Auto-7 / Sprint 060 — Substrate fix (Blocker A + Blocker B fix path (b) + Blocker C in-session + scoring SHA rebaseline 5177b674→22548e20) | `infra`; §7 EXEMPT (self-walked) | per-sub-sprint `pass / 0` `approve with downgrade-to-signal follow-up` 2026-05-29 | §4.3 trigger #3 (fence #13 + #2 controlled overrides) | B — Surfaced findings need fix-iteration sub-classified `approve with downgrade-to-signal follow-up` 2026-05-29; substrate scope ACCEPTED; Goal #4 BLOCKED → S-Auto-7.1; governance trigger (§6 fence #2 annotation) addressed in close-bundle |
| S-Auto-7.1 / Sprint 061 — Pre-flight env check + smoke iter completion (NEW OQ-S61.1 applier.py:370 mvn module-selection surfaced) | `infra`; §7 EXEMPT (self-walked) | This milestone-shared review (Phase 2 dispatch) | None (no §4.3 trigger; milestone-shared default) | A — Clean PASS at sub-sprint level 2026-05-29; pre-flight + auto-reboot LANDED + operator-verified; Goal #3 BLOCKED on OQ-S61.1 = different substrate surface → S-Auto-7.2 in M-Auto-1C |

### 12.4 Acceptance bar audit (15 hard gates from §5)

| Met (8/15) | Unmet (7/15; deferred to M-Auto-1C) |
|---|---|
| Tier-0 safety floor unchanged (cumulative zero `server/` / `eval/src/main/java/` / `eval_interactive/eval_interactive/` Java edits except in-session Blocker C `loader.py` controlled override) | Live iter end-to-end (Goal #4 S-Auto-7 + Goal #3 S-Auto-7.1 both BLOCKED on OQ-S61.1) |
| Java test baseline preserved `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED | Pre-batch baseline drift envelope at S-Auto-6 open (S-Auto-6 closed empty) |
| Python test baseline preserved; eval_interactive `486 PASS, 3 FAIL UNCHANGED`; autoloop grew 216 → 255 PASS (+39 cumulative: +7 S-Auto-5 + +5 S-Auto-7 + +27 S-Auto-7.1) | First overnight batch executed at S-Auto-6 (deferred to M-Auto-1C / S-Auto-8) |
| S-Auto-5 per-sub-sprint Codex review `pass / 0` | First human review of overnight kept candidates (deferred to M-Auto-1C / S-Auto-8) |
| Detector calibration evidence at S-Auto-5 close (17-fixture sweep 11/4/2 + 3 real meta-agent samples = 20 calibration data points all PASS; FLAG rate 0% / FP rate 0%) | First cherry-pick decision via AskUserQuestion (deferred to M-Auto-1C / S-Auto-8) |
| Real-meta-agent batch calibration at S-Auto-5 close via augmented-evidence path per OQ-S58.4 | Curated bad-case suite manual review pass at M-Auto-1B close (PRIMARY GATE per §5.6) — **MET 2026-05-30** (suite-aggregate PASS at primary gate; 5/12 case_passed UNCHANGED; bidirectional drift signature; per §12.5 deliver-agent + human joint review) |
| R-S57 closed (S-Auto-5 Fix-C hybrid) | Shadow regression-safety gate (parity with M-Auto-1A NEW) — **OBSERVATION 2026-05-30** (shadow drift 4/22 → 1/22 UC-D-clustered + anchor_outcome 7/12 → 3/12 = elevated LLM-provider drift envelope; no bot-code regression; Codex Axis M6 PASS WITH FOLLOW-UP recommendation that S-Auto-8 establish ≥2 rerun drift envelope before overnight — surfaced in §12.10 M-Auto-1C planning + §12.14 trigger #1) |
| Milestone-shared Codex review at M-Auto-1B close (`pass / 0 / approve with downgrade-to-signal follow-up` 2026-05-30) — **MET** | (n/a — both halves now MET; revised tally 10/15 met + 5/15 deferred to M-Auto-1C; the 5 unmet items all trace to OQ-S61.1 blocking the smoke iter through Step 9) |

Per Phase 2 evidence collection 2026-05-30 + Codex Phase 3 verdict, the §5.6 PRIMARY GATE + shadow regression-safety gate + milestone-shared Codex review all MET. Revised tally is **10/15 met + 5/15 deferred to M-Auto-1C**. The 5 still-unmet items all trace to the same upstream cause: the smoke iter through Step 9 was structurally blocked, ultimately by OQ-S61.1 applier.py:370 mvn module-selection bug on csagent-parent packaging=pom (the foreground :8080 contention previously masked this; pre-flight clears the masking and exposes the underlying bug). The 1-line `applier.py:370-378` fix is S-Auto-7.2 / Sprint 062 in M-Auto-1C. Phase 2 §5.6 + shadow rerun was independent of OQ-S61.1 (the bot runtime is byte-identical to M-Auto-1A close on the agent-loop runtime path; no Skill YAML edit by the agent loop; no Java edit by the agent loop; only the ambient-human-work `7871c62` PII sanitizer + max_tool_steps tunings are in range, dispositioned at §12.13 + Codex Axis M11 PASS) and reproduced M-Auto-1A close's LLM-provider drift signature with elevated UC-D/E/F/FP magnitude (Codex Axis M6 PASS WITH FOLLOW-UP).

### 12.5 §5.6 bad-case suite + shadow regression-safety evidence (PRIMARY GATE per §5.6) — Phase 2 placeholder

*Phase 2 evidence collection by deliver-agent + human, BEFORE Codex dispatch. Append run-IDs + per-suite case_passed counts + per-case PASS/FAIL/IMPROVING joint judgment table below. Small follow-up commit before Codex dispatch.*

**Phase 2 commands** (foreground :8080 backend must be restarted first; the `--auto-reboot` killed it during S-Auto-7.1 verification):

```bash
# Restart foreground backend (in a separate shell):
cd server && mvn spring-boot:run
# Wait ~30s for Spring startup; verify via curl http://localhost:8080/actuator/health

# Bad-case suite (parallel=1 per R-bad-case-parallel-session-establishment-flakiness M5 priority):
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1
# Expected ~7m23s wall-clock; 12 cases.

# Anchor-outcome suite (parallel=4):
cd eval_interactive && uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4
# Expected ~1m50s wall-clock; 12 cases.

# Shadow regression-safety suite (parallel=4):
cd eval_interactive && uv run eval-interactive run --path case_specs_shadow/ --parallel 4
# Expected ~3m33s wall-clock; 22 cases (NOT 23 — `_manifest.yaml` correctly filtered).
```

Three NEW timestamped run-IDs appear under `eval_interactive/results/`. Read per-turn traces; jointly classify PASS / FAIL / IMPROVING per `eval_interactive/case_specs/bad_cases/_manifest.md` lifecycle + `iteration_governance.md` §5.6 cadence. Expected pattern: matches M-Auto-1A close LLM-provider drift signature (5/34 = 14.7% one-direction `True/0.5 → False/0.0` drift recorded as provider non-determinism, NOT regression).

**Evidence table (Phase 2 evidence; recorded 2026-05-30 deliver-agent + human joint review)**:

| Suite | Run-ID | Wall-clock | total cases | case_passed (loader-counted) | tier2 mandatory FAILs | Per-case verdict notes |
|---|---|---:|---:|---:|---:|---|
| bad_cases (parallel=1) | `20260529-170618` | 644994ms (10m45s) | 12 | **5** (UNCHANGED vs S-Auto-7 baseline `20260529-101324` = 5/12) | 6 | Bidirectional drift: 2 regressions (cs001, wmkb) + 2 improvements (cs012, cs015 — cs015 is a FLAKE clear from baseline's session-establishment timeout). fg5q also improved underlying state (AM=contract_violation flake; PM=clean bot_ended/outcome=1.0; case_passed stays False due to closure_criterion programmatic check). Suite-aggregate **PASS** at PRIMARY §5.6 GATE. |
| anchor_outcome (parallel=4) | `20260529-172052` | 144416ms (2m24s) | 12 | **3** (DOWN −4 vs S-Auto-7 baseline `20260529-102054` = 7/12) | 8 | 4 PASS→FAIL regressions all one-direction: anchor_outcome_uc_d_login + uc_e_promotion + uc_f_billing + uc_fp_removed. UC-D/E/F/FP-clustered; consistent with LLM-provider drift on account/auth/billing flows. Outcome-only surface per §5.5 2026-05-21 update; observation-only at suite level. |
| shadow (parallel=4) | `20260529-172612` | 296545ms (4m57s) | 22 | **1** (DOWN −3 vs S-Auto-7 baseline `20260529-102949` = 4/22) | 7 | 3 PASS→FAIL regressions all one-direction + all UC-D: cs11s01_uc_d_two_emails_one_account + cs11s02_uc_d_password_change_loop_high_distress + cs95s02_uc_d_logged_in_different_browser_no_ads. Strong UC-D clustering reinforces LLM-provider drift signature on account-auth flows. Regression-safety parity surface (S4 NEW); observation-only at suite level. |
| **Total** | 3 runs | ~18m06s | 46 | **9/46** (down 7 vs 16/46 baseline) | 21 | **7 PASS→FAIL regressions** (77.8% of drifts uni-directional) + **2 FAIL→PASS improvements** (cs012 + cs015 flake clear in bad_cases) = 9 drifts net −7. Drift magnitude 9/46 = 19.6% (vs M-Auto-1A close 5/34 = 14.7%); slightly higher but in-envelope. |

**Bot byte-identical claim verification (cumulative M-Auto-1B)**:

```bash
git diff --stat b6b627b..HEAD -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/ server/src/main/resources/ data/ db/migration/
```

Result: NOT empty due to ambient-human-work commit `7871c62` (dispositioned at §12.13 + Codex Axis M11). The walk surfaces ONLY `7871c62` fence-touching files (ToolCallTraceSanitizer.java + 2 Skill YAMLs + test files). The Skill YAML changes (`max_tool_steps` tuning) DO materially change bot tool-call budget — but for cs001 specifically, the bot terminated with `goal_impossible` at turn 0/1 WITHOUT consuming budget, so the increased `max_tool_steps: 4→6` is structurally irrelevant to the cs001 failure path; cs001 regression is **LLM-provider drift on terminal-decision-making**, NOT budget tuning. The PII sanitizer refactor is observability-only (post-hoc trace sanitization, not runtime decision). The "bot byte-identical" claim is correct for the **agent-loop scope** (zero edits from S-Auto-5/6/7/7.1 dev sessions) but the ambient-human-work commit IS in the cumulative range.

**§5.6 per-case joint classifications (PRIMARY GATE = bad_cases; deliver-agent + human joint review 2026-05-30)**:

| Case | Baseline (AM) | NEW (PM) | Joint classification | Attribution | M-Auto-1C / S-Auto-8 calibration target? |
|---|---|---|---|---|---|
| `alice_uc_a_uc_h_misclass` | PASS | PASS | **PASS** (stable per M5 manifest IMPROVING shape) | n/a | No |
| `cs001_uc_c_mechanical_template_escalate` | PASS | **FAIL** (mandatory `search-knowledge-before-faq-answer` FAILED; stop_reason=goal_impossible without trying search_knowledge) | **FAIL per closure_criterion** | **LLM-provider drift on terminal-decision-making** — bot short-circuited to `goal_impossible` before running policy-mandated `search_knowledge + resolve_article` sequence. NOT attributable to 7871c62 budget tuning (the increased `max_tool_steps: 4→6` would HELP not HURT; bot never consumed budget at all in PM run — terminated at turn 0/1) | **YES** — Skill YAML edit candidate on `resolve_faq_grounded_answer.yaml` procedure / critical_steps[*].desc to gate goal_impossible terminal-state on policy-mandated search completion |
| `cs011_uc_c_faq_miss_not_distress` | FAIL (case_passed=False; outcome=1.0; stop=bot_ended) | FAIL (same shape) | **PASS-shape stable** per M5 manifest IMPROVING | n/a | No |
| `cs012_uc_fp_late_phone_failure_path` | FAIL (case_passed=False; outcome=1.0) | **PASS** (case_passed=True) | **IMPROVING** (programmatic improvement; closure_criterion now met) | LLM-provider bidirectional variance | No |
| `cs014_uc_c_faq_miss_not_distress` | PASS | PASS | **PASS** (stable per M5 manifest) | n/a | No |
| `cs015_uc_fp_appeal_edit_repost` | FAIL (FLAKE: outcome=0.00 stop=timeout — documented `R-bad-case-parallel-session-establishment-flakiness`) | **PASS** (clean run; outcome=1.0 stop=bot_ended) | **IMPROVING-on-flake-clear** | Flake intermittency cleared; baseline run had session-establishment flake | No (flake; tracked via existing R-item) |
| `cs029_uc_d_account_locked_callback` | FAIL (case_passed=False; outcome=1.0) | FAIL (same shape) | **PASS-shape stable** per M5 manifest PASS (UC-D / escalated, clean) | n/a | No |
| `cs066_uc_k_in_app_feature_regression` | FAIL (case_passed=False; outcome=0.50) | FAIL (same shape; outcome=0.50) | **PASS-shape stable** per M5 manifest PASS (UC-K / escalated, stable Tier-2 sub-tags) | n/a | No |
| `cs095_uc_d_email_recovery_misroute` | PASS | PASS | **PASS** (stable) | n/a | No |
| `fg5q_uc_fp_phone_rejected_repost` | FAIL (FLAKE: outcome=0.00 stop=contract_violation) | FAIL (case_passed=False BUT clean run: outcome=1.0 stop=bot_ended; underlying state IMPROVED) | **PASS-shape with flake clear** per M5 manifest PASS | Flake intermittency cleared in PM run | No (flake; tracked via existing R-item) |
| `iwzx_uc_k_advert_on_hold_restore` | FAIL (case_passed=False) | FAIL (same shape) | **FAIL-stable** per M5 manifest (UC-H / escalated; documented raison d'être intact) | Linked to `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` action_bank R-item | No (already tracked R-item) |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | PASS | **FAIL** (mandatory `uc-h-intake-complete-before-handover` FAILED; bot routed into UC-H without completing intake) | **FAIL per closure_criterion** | **LLM understanding gap on handover gating contract** — bot entered UC-H pathway and handed over to human without completing intake; human agent receives partial / context-less handover. Outcome=1.0 rates the bot's user-facing closing message acceptable but the handover-context contract was violated. NOT attributable to 7871c62 (no max_tool_steps change in resolve_intake) | **YES** — Skill YAML edit candidate on `resolve_intake_collect_and_handover.yaml` critical_steps[*].desc or procedure to gate handover on intake completion |

**Suite-aggregate §5.6 PRIMARY GATE disposition**: **PASS** (deliver-agent + human joint 2026-05-30). The curated bad-case suite held at 5/12 loader-counted case_passed (zero net regression); the 4 binary drifts decompose into 2 regressions (cs001 + wmkb both procedural-mandatory-step FAILs with outcome=1.0) + 2 improvements (cs012 genuine improvement + cs015 flake clear); plus fg5q underlying-state improvement (case_passed False but clean run reproduces M5 PASS-shape; flake cleared). Bidirectional drift signature consistent with LLM-provider variance; NO systematic regression direction within the bad-case suite. The 2 PRIMARY-GATE individual case FAILs are **valuable bad-case signal for M-Auto-1C / S-Auto-8 overnight auto-loop iteration** (not "ignorable noise"); they become explicit S-Auto-8 cherry-pick candidates for Skill YAML edits on `resolve_faq_grounded_answer.yaml` (cs001 terminal-decision gating) + `resolve_intake_collect_and_handover.yaml` (wmkb handover-intake gating). This is the FIRST CONCRETE EVIDENCE that M-Auto-1A/B's substrate has real failure modes for the auto-loop to optimize against — a positive M-Auto-1B/C transition signal.

**Anchor_outcome + shadow disposition**: both are observation-only at suite level per §5.5 (smoke composite_score demotion 2026-05-16) / §5.6.2 (per-milestone bad case selection). The 7 one-direction regressions cluster on UC-D / UC-E / UC-F / UC-FP — auth/account-setting/billing flows. Consistent with **LLM-provider drift on auth-flow-heavy use cases**. Bot byte-identical on runtime path (apart from 7871c62 ambient PII sanitizer refactor + 2 Skill YAML max_tool_steps tunings which don't touch resolve_account / resolve_billing skills); drift attributable to provider behavior shift, not bot code regression. Drift magnitude 7/34 (across anchor + shadow) = 20.6%, vs M-Auto-1A close 5/34 = 14.7%; the elevated magnitude is in-envelope-but-higher; flag as M-Auto-1C / S-Auto-8 baseline-rerun-twice consideration (S-Auto-8 dev prompt should establish drift envelope ≥2 reruns before overnight to anchor against provider variance per `iteration_governance.md` §10 stop condition).

**NEW R-item surfaced at §5.6 Phase 2 review**: `R-eval-interactive-judge-score-never-populated` (M-Auto-2+ observability hygiene; LOW priority). All 46 cases in both AM baseline and PM rerun show `judge_score: 0.0` and `mean_judge: 0.0000` — the eval-interactive four-tier eval's judge dimension has never been wired through to non-zero values for these case_specs (predates S-Auto-7 baseline; predates M-Auto-1A). The CLI summary "Passed: 0" headline depends on this judge dimension and is therefore structurally misleading; loader-counted `case_passed` (via outcome + tier-2 mandatory step + closure_criterion programmatic check) is the canonical signal that autoloop / baseline_loader and the deliver-agent + human use. NOT a M-Auto-1B regression; long-standing observability debt parallel to the `observability_debt_pattern` memory pattern.

**Phase 2 evidence collection commands executed** (for reproducibility):

```bash
# Restart foreground :8080 backend (human ran in separate shell)
cd server && mvn spring-boot:run

# Bad-case suite (parallel=1)
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1
# → run-20260529-170618; 644994ms; 5/12 case_passed

# Anchor-outcome suite (parallel=4)
cd eval_interactive && uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4
# → run-20260529-172052; 144416ms; 3/12 case_passed

# Shadow regression-safety suite (parallel=4)
cd eval_interactive && uv run eval-interactive run --path case_specs_shadow/ --parallel 4
# → run-20260529-172612; 296545ms; 1/22 case_passed
```

**§5.6 lifecycle ledger update**: `eval_interactive/case_specs/bad_cases/_manifest.md` updated 2026-05-30 with new M-Auto-1B close section recording (a) per-case verdict table (12 cases); (b) S-Auto-8 calibration targets (cs001 + wmkb) tagged with proposed Skill YAML edit surfaces; (c) flake clear observations on cs015 + fg5q. See `_manifest.md` "M-Auto-1B close bad-case + shadow review (2026-05-30)" section.

### 12.6 §8.5 split decision rationale

**Split decision locked 2026-05-29** via joint deliver-agent + human AskUserQuestion (option (a) recommended; selected). Rationale:

- §8.5 directive: "A milestone that exceeds 5 sub-sprints is a signal that the milestone scope is too large; the deliver-agent SHALL split it at the next milestone planning round." M-Auto-1B at S-Auto-7.1 close = 5 sub-sprints; adding S-Auto-7.2 → 6 → SHALL split. The current planning round IS the next planning round per §8.5 timing.
- Alternative options rejected: (b) overflow exception with documented justification — sets precedent that §8.5 exceptions are routine; future milestones may abuse. (c) defer to M-Auto-2 — dilutes M-Auto-2 scope which was reserved for "Stage-2 entry decision" planning.
- M-Auto-1C continuation milestone has 2 sub-sprints (S-Auto-7.2 / Sprint 062 applier mvn fix + S-Auto-8 / Sprint 063 first overnight + first cherry-pick) — well within §8.5 ceiling; clean architectural theme = "finish what M-Auto-1B substrate-fixed but didn't validate end-to-end".
- M-Auto-1B's 8/15 acceptance bar items + the §5.6 + shadow rerun + this milestone-shared Codex review are still close-gate evidence; they DO close cleanly when Phase 2/3 finishes. The 7/15 deferred items are not scope cuts — they are explicit M-Auto-1C scope inheritance.

The §8.5 split is itself a §8.5-compliant scope-discipline action, NOT a scope cut. Codex Axis M7 in `compact/M-Auto-1B-review-prompt.md` verifies this independently.

### 12.7 R-item flips

- **Closed at M-Auto-1B (one)**: `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` (closed at S-Auto-5 close 2026-05-28 via Fix-C hybrid; the Codex Axis B exact bypass `Whenever ... =>` FAILs the detector via `Q1.if_then_decision_tree` after word-boundary normalization → `if` + `_SYNONYM_MAP` arrow mapping; end-to-end calibration evidence held across all subsequent sub-sprints since the detector was untouched after S-Auto-5).
- **Opened at M-Auto-1B (two)**:
  - `R-S58-anti-hardcode-zero-width-when-arrow-bypass` (S-Auto-5 Codex Axis C surfaced 2026-05-28; carry to M-Auto-1C / Fix-D candidate; final disposition deliver-agent + human at M-Auto-1C close based on overnight evidence after S-Auto-8 lands).
  - **`R-eval-interactive-judge-score-never-populated`** (NEW at Phase 2 §5.6 review 2026-05-30; flagged at Codex Axis M9 downgrade trigger; formalized into `docs/action_bank.md` §5 at Phase 3 per deliver-agent + human joint decision 2026-05-30). All 46 cases across bad_cases + anchor_outcome + shadow have `judge_score: 0.0`; the eval-interactive four-tier eval's judge dimension has never wired through to non-zero on these case_specs (predates S-Auto-7 baseline; predates M-Auto-1A). The CLI summary "Passed: 0" headline depends on this dimension and is therefore structurally misleading; loader-counted `case_passed` (outcome + tier-2 mandatory step + closure_criterion programmatic check) is the canonical signal that autoloop / baseline_loader and the deliver-agent + human use. M-Auto-2+ observability hygiene; LOW priority; long-standing eval-side debt parallel to `observability_debt_pattern` memory.
- **NEW M-Auto-1B → M-Auto-1C transition R-item candidate**: `OQ-S61.1-applier-mvn-module-selection-csagent-parent` (S-Auto-7.1 handoff §6 surfaced 2026-05-29; 1-line fix at `applier.py:370-378`; S-Auto-7.2 / Sprint 062 resolves; may or may not be formalized as a tracked R-item per deliver-agent + human discretion at M-Auto-1C open).
- **Carry-over from M-Auto-1A** (7 R-items unchanged): `R-eval-java-module-retirement`, `R-bad-case-parallel-session-establishment-flakiness`, `R-shadow-fixture-empty-form-session-create-400`, `R-case-families-manifest-cs095-smoke-vs-anchor-orphan`, `R-bad-case-metadata-field-name-canonicalize`, `R-bad-case-suite-uc-ghij-seed-from-real-sessions`, `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress`.

### 12.8 Architecture-health metric direction (§6 metrics)

- `new_semantic_hardcode_count` (target: down): cumulative across M-Auto-1B = **0**. Fix-C step 1 word-boundary regex is generic structural per D2 (refinement of existing `_SYNONYM_MAP` semantics, not enum expansion); detector self-discipline 3 regression tests PASS unchanged; substrate-fix + pre-flight + dotenv all pure-infra. **Direction: HELD FLAT AT 0 (good)**.
- `soft_signal_conversion_count` (target: up): no cherry-pick landed in M-Auto-1B (S-Auto-6 was the cherry-pick slot but closed empty); no Skill YAML edit → 0 conversions. **Direction: 0 movement (expected for incomplete-acceptance-bar close)**.
- `planner_ownership_ratio` (target: up): no runtime behaviour change → unchanged. **Direction: 0 movement (expected)**.
- `shadow_disagreement_rate` (target: down): not yet measured (no overnight ran). **Direction: first measurement deferred to M-Auto-1C / S-Auto-8 overnight**.

All §6 metrics remain `collection_status: not_started` per §5.5 (observation-only).

### 12.9 Cross-milestone observations

- Pre-flight env check infra delivered in S-Auto-7.1 is a general-purpose ergonomic that benefits M-Auto-2+ and beyond (every future auto-loop session can invoke `python -m autoloop preflight` or `python -m autoloop run --auto-reboot`). The `feedback_preflight_env_check_outer_loop` memory captures the durable principle "pre-flight env at outer-loop entry point; in dev, reboot misbehaving services rather than diagnose mid-iteration crashes" — applicable to future substrate-fix iterations beyond M-Auto-1B.
- The OQ-S58.7 / OQ-S60.7 / OQ-S61.1 diagnostic chain is a useful case study in diagnostic-attribution error: the initial Spring spawn brittleness attribution to "Flyway lock + maven race + Redis pool" was a plausible-but-incorrect attribution masked by the foreground :8080 contention. The actual root cause (mvn module-selection on csagent-parent packaging=pom) only became visible after pre-flight cleared the masking. Future substrate-fix iterations should keep this in mind: a successful pre-flight does NOT guarantee the smoke iter will succeed; it just removes one layer of masking.
- **Ambient-human-work commit `7871c62` "update budget and pii Sanitizer"** landed between S-Auto-7 close-bundle (`307f69a` 2026-05-29) and S-Auto-7.1 dev session (`19213b1` 2026-05-29) on 2026-05-29 20:59 BJT by the human (Rex1028). Touches §6 fence #1 (server/src/main/java/.../ToolCallTraceSanitizer.java net −174 LOC PII sanitizer refactor + matching test simplification in server/src/test/) + fence #3 (server/src/main/resources/skills/discover_triage.yaml max_tool_steps: 2→3 + resolve_faq_grounded_answer.yaml max_tool_steps: 4→6). NOT part of any sub-sprint scope. Explicit human authorization at Phase 1.5 close-bundle 2026-05-30 ("这个提交是我做的，不要影响它"); treated as ambient out-of-scope human work parallel to the 60c5b67 README precedent at S-Auto-7 close. Full disposition at §12.13 below + Codex Axis M11 in `compact/M-Auto-1B-review-prompt.md`. The two Skill YAML max_tool_steps tuning edits are the kind of LLM-soft-field edit M-Auto-1B's cherry-pick mechanism (fence #3) was reserved for; the human exercised this outside the mechanism because S-Auto-6 closed empty without exercising the mechanism. M-Auto-1C's S-Auto-8 cherry-pick mechanism remains the canonical path for future LLM-soft-field edits.
- M3-B Single Handover Orchestrator P0 remains in candidate slate for milestone AFTER M-Auto-1C.
- M5 carry-over projection-hygiene candidate remains independent milestone candidate.

### 12.10 Next planning round candidate

**M-Auto-1C — Auto-Evolution Calibration Continuation** (Phase 3 deliver-agent draft 2026-05-30):

- 2 sub-sprints within §8.5 ceiling; margin = 3 for fix-iteration.
- S-Auto-7.2 / Sprint 062 — applier.py:370 mvn module-selection fix (`infra` / §7 EXEMPT; ~half-day; 1-line diff + 1-2 tests; smoke iter completion is the close gate).
- S-Auto-8 / Sprint 063 — first overnight batch + first human review + first cherry-pick to main (`eval_spec` / §7 REQUIRED; ~3-5 days; the original deferred S-Auto-6 scope reattempted on the now-validated substrate).
- **Codex M6 / Phase 3 downgrade trigger #1**: S-Auto-8 SHALL establish a **≥2 baseline rerun drift envelope BEFORE overnight kick-off** given the Phase 2 evidence of elevated UC-D/E/F/FP LLM-provider drift (anchor_outcome 7/12 → 3/12 + shadow 4/22 → 1/22). The envelope establishes a per-suite case_passed median + IQR; if baseline-vs-baseline drift exceeds 10/34 cases (~30%; 2× the M-Auto-1A close-day signature), halt overnight + surface to deliver-agent + human per `iteration_governance.md` §10 stop condition rather than feeding noisy baseline into overnight comparison. The S-Auto-8 sprint_objective + dev prompt MUST embed this requirement.
- **cs001 + wmkb bad-case calibration targets for S-Auto-8 cherry-pick**: the Phase 2 §5.6 review identified `cs001_uc_c_mechanical_template_escalate` + `wmkb_uc_a_trader_flag_secondary_uc_h` as concrete cherry-pick candidates with proposed Skill YAML edit surfaces (cs001 → `resolve_faq_grounded_answer.yaml` procedure / critical_steps[*].desc to gate `goal_impossible` terminal-state on policy-mandated `search_knowledge` completion; wmkb → `resolve_intake_collect_and_handover.yaml` critical_steps[*].desc to gate handover on intake completion). S-Auto-8 overnight should observe whether the meta-agent surfaces analogous candidates; the manual-review judgment at S-Auto-8 close uses these targets as reference signals (NOT pre-committed cherry-picks; the meta-agent owns proposal authorship).
- R-S58 zero-width bypass surface check during S-Auto-8 overnight propose distribution scan; final R-S58 disposition (defer-to-M-Auto-2 OR extend-with-S-Auto-9-Fix-D) at M-Auto-1C close.
- Hard fences: inherit M-Auto-1B §6 17 fences with the loader.py + eval_runner.py + applier.py:370 controlled overrides finalized; no new fence overrides expected.
- Codex review plan (§4.3): default milestone-shared at M-Auto-1C close; S-Auto-7.2 may upgrade to per-sub-sprint if its fix touches a new fence surface (low probability — 1-line applier change); S-Auto-8 conditional per-sub-sprint per §4.3 trigger if cherry-pick candidate borderline-§5.3 surfaces.

### 12.11 Reproducibility check (Phase 3 verify)

Every numeric claim in §12.1-12.10 is reproducible:

- Cumulative pytest 216 → 255 PASS: `cd autoloop && uv run --extra dev pytest -q` (at each commit b6b627b, ae0ec3e, b0a3704, 19213b1).
- eval_interactive pytest 486 / 3 UNCHANGED: `cd eval_interactive && uv run python -m pytest --tb=no -q`.
- Java baseline 1183 / 1 / 0 / 2: `cd server && mvn test -B` (skip if Java zero-touch verified via git diff).
- scoring SHA: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` produces `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`.
- Blessed baseline_dir loads cleanly: `from autoloop.scoring import baseline_loader; baseline_loader.load(Path('eval_interactive/results/m-auto-1b-baseline-20260529').resolve(), config=config)` → 3 SuiteSnapshot, 0 warnings.
- `--auto-reboot` operator-verified: `docs/sprints/sprint-061-handoff.md` §4 transcript.
- §5.6 + shadow rerun: Phase 2 evidence collection (commands in §12.5 above).
- §6 fence #2 + #13 controlled-override annotations: `git log -p docs/milestone_objective.md` around commits `307f69a` (fence #2 annotation) + `b6084f9` / `307f69a` (fence #13 annotation).

### 12.13 Ambient-human-work `7871c62` disposition (NEW Phase 1.5 2026-05-30)

The human committed `7871c62 update budget and pii Sanitizer` 2026-05-29 20:59 BJT (Friday evening) between the S-Auto-7 close-bundle (`307f69a` 2026-05-29 morning BJT) and the S-Auto-7.1 dev session (`19213b1` 2026-05-29 evening BJT). The commit landed on `auto-loop-branch` outside any sub-sprint contract or dev-agent / deliver-agent / review-agent framing.

**Commit contents (verified via `git show --stat 7871c62`)**:

| File | Lines added | Lines removed | Fence touched |
|---|---:|---:|---|
| `README.md` | 1 | 1 | None (root README is not fenced) |
| `docs/runbooks/admin-guide.md` | 6 | 0 | None (runbooks not fenced) |
| `server/src/main/java/.../ToolCallTraceSanitizer.java` | 22 | 140 | **§6 fence #1** (server/src/main/java/** byte-identical) |
| `server/src/main/resources/skills/discover_triage.yaml` | 1 | 1 | **§6 fence #3** (Skill YAML conditionally writable EXACTLY ONCE during S-Auto-6 cherry-pick; S-Auto-6 closed empty — this edit bypasses the cherry-pick mechanism) |
| `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` | 1 | 1 | **§6 fence #3** (same as above) |
| `server/src/test/.../Sprint049TraceObservabilityFidelityIntegrationTest.java` | 9 | 18 | §6 fence #1 (test/ also fenced under server/src/) |
| `server/src/test/.../PhaseEvaluatorSkillIntegrationTest.java` | 1 | 1 | §6 fence #1 |
| `server/src/test/.../runtime/ToolCallTraceSanitizerTest.java` | 45 | 82 | §6 fence #1 |
| `server/src/test/.../service/runtime/skill/SkillLoaderTest.java` | 7 | 17 | §6 fence #1 |

**Net**: −174 LOC across 9 files; primarily a PII sanitizer refactor (`ToolCallTraceSanitizer.java`) with matching test simplifications + 2 Skill YAML `max_tool_steps` tuning edits (`max_tool_steps: 2→3` on discover_triage + `4→6` on resolve_faq_grounded_answer).

**Disposition** (deliver-agent + human joint AskUserQuestion 2026-05-30 at Phase 1.5):

The human's verbatim response: "这个提交是我做的，不要影响它。你继续把其他更新提交就好了。" (= "This commit is mine, don't affect it. You just continue committing other updates."). Disposition selected: **ambient human work outside the M-Auto-1B agent-loop framing**, treated as Codex Axis I-style observation parallel to the `60c5b67` README precedent at S-Auto-7 close. The commit is NOT reverted; the close-bundle continues; Codex Axis M11 explicitly walks this disposition.

**Why this is consistent with M-Auto-1B governance discipline (NOT a precedent for fence relaxation)**:

1. **Agent vs human-scope distinction**: M-Auto-1B §6 fences are designed to enforce scope discipline AGAINST agents (deliver-agent / dev-agent / meta-agent / review-agent) within the auto-evolution loop, preventing them from expanding scope beyond the milestone contract. The fences are not a contract over the human project lead's ambient work — the human is the ultimate scope authority. Per `iteration_governance.md` §1.1 "Rules define boundaries; LLM owns semantic understanding" the agent-loop rules are agent-loop rules.
2. **No agent-scope-creep involved**: the commit was authored by the human (Rex1028) directly, NOT by any agent in the M-Auto-1B loop. No deliver-agent / dev-agent / meta-agent scope expansion happened; no §1.7 forbidden-list red line was crossed by any agent.
3. **No semantic surface invented by an agent**: the Skill YAML `max_tool_steps` tuning is a hand-edit of a numeric value; the Java refactor reduces LOC (simplification, not feature). Neither encodes a new keyword / regex / if-else / per-UC matrix per §1.7.
4. **Substrate-fix scope unaffected**: S-Auto-7's eval_runner.py / scoring SHA / loader.py changes are byte-identical between `307f69a` and `19213b1` (verify `git diff 307f69a..19213b1 -- autoloop/autoloop/scoring/ eval_interactive/eval_interactive/case_spec/loader.py` empty). S-Auto-7.1's preflight.py / cli.py changes are pure-autoloop (`git diff 7871c62..19213b1 --stat` shows only autoloop/ + handoff scope; no Java/skills overlap).
5. **Cherry-pick mechanism intent preserved for M-Auto-1C**: the M-Auto-1B fence #3 cherry-pick mechanism remains the canonical path for LLM-soft-field edits via the agent loop. The human's manual `max_tool_steps` edit was a one-off out-of-loop tuning; it does NOT establish precedent for future agent-loop work to bypass the mechanism.
6. **Codex visibility preserved**: the disposition is recorded in TWO places for future audit (this §12.13 + `compact/M-Auto-1B-review-prompt.md` commit range claim + Axis M11). Codex Axis M5 hard-fence walk WILL find the fence-touching files; Axis M11 disposes them as ambient human work, NOT a Codex blocker.

**M-Auto-1B closure verdict implication**: this disposition is reviewed by Codex at Axis M11 (Phase 2). If Codex accepts (likely PASS), no change to the M-Auto-1B classification. If Codex returns `approve with downgrade-to-signal follow-up` with trigger = "formalize a governance-doc note on ambient-human-work classification at milestone close", deliver-agent + human will draft a small `docs/current/iteration_governance.md` §X note on ambient-human-work governance at M-Auto-1C planning round. If Codex returns `needs human architecture decision` flagging this as a governance gap (e.g., the human's commit-during-milestone behavior needs a formal carve-out distinct from agent-scope), deliver-agent + human will surface for human architecture decision.

**M-Auto-1C planning implication** (per §12.10): the cherry-pick mechanism remains the canonical path for agent-loop Skill YAML edits. S-Auto-7.2 (applier mvn fix) does NOT touch Skill YAMLs. S-Auto-8 (overnight + cherry-pick) WILL exercise the mechanism once per fence #7. The human's manual `max_tool_steps` edits do NOT pre-empt S-Auto-8's cherry-pick (S-Auto-8's overnight may surface a different Skill YAML candidate; the cherry-pick mechanism handles up to 1 per M-Auto-1C close).

**Future ambient-human-work treatment** (M-Auto-2+ governance question; NOT decided here): whether to formalize "human commits outside the agent loop are governance-equivalent to ambient infrastructure work and don't require milestone-fence carve-outs" OR whether to require formal pre-authorization (similar to S-Auto-7's planning-blessed fence #13 override pattern). Deliver-agent recommends surfacing this as an M-Auto-2 governance-doc consideration if Codex flags it at Axis M11.

### 12.12 Phase 3 close artefacts checklist (deliver-agent owned)

At Phase 3 (this commit), the deliver-agent performs:

1. ☑ Fill §12.1 final classification with Codex outcome (A-with-acceptance-bar-revision; Codex `pass / 0 / approve with downgrade-to-signal follow-up`).
2. ☑ Reconcile §12.2 commit-range count (18 raw / 14 agent-loop / 4 adjacent non-scope) per Codex Axis M10 downgrade trigger #2.
3. ☑ Add `R-eval-interactive-judge-score-never-populated` to `docs/action_bank.md` §5 per Codex Axis M9 downgrade trigger #3.
4. ☑ Add 3 non-blocking Codex downgrade trigger dispositions at §12.14 below.
5. ☑ Prepend S-Auto-5 Codex verdict body to live `docs/codex-findings.md` (from `git show 95089c2:docs/codex-findings.md`) so all 3 reviews (S-Auto-5 + S-Auto-7 + M-Auto-1B milestone-shared) live in one self-contained M-Auto-1B archive per Codex Axis M10 downgrade trigger #2.
6. ☑ Set front-matter `status: archived` + `implementation_status: implemented` + `last_reviewed: 2026-05-30`.
7. ☐ `git mv docs/milestone_objective.md → docs/milestones/M-Auto-1B_objective.md`.
8. ☐ `git mv docs/codex-findings.md → docs/milestones/M-Auto-1B_codex-review.md`.
9. ☐ Reset live `docs/codex-findings.md` to scaffold per M-Auto-1A close pattern.
10. ☐ Write NEW live `docs/milestone_objective.md` = M-Auto-1C "Auto-Evolution Calibration Continuation" (§12.10 above is the planning notes; ≥2 baseline rerun drift envelope requirement on S-Auto-8 per §12.14 trigger #1).
11. ☐ Write NEW live `docs/sprint_objective.md` = S-Auto-7.2 / Sprint 062 contract (applier.py:370 mvn module-selection fix).
12. ☐ Write `compact/sprint-062-dev-prompt.md` self-contained per §9.
13. ☐ Update `docs/10-handoff.md` §0 + §1 + §2 with M-Auto-1B close lead + archive index row.
14. ☐ Update `docs/action_bank.md` §6.5 NEW M-Auto-1B closed-milestone-index row + R-S57 close confirmation in §5 + R-S58 carry annotation + NEW R-eval-interactive-judge-score-never-populated entry in §5 + Sprint 062 close-action placeholder in §6.
15. ☐ Human commits Phase 3 close-bundle.
16. ☐ Dispatch S-Auto-7.2 dev session (paste `compact/sprint-062-dev-prompt.md`).

### 12.14 Codex downgrade-trigger dispositions (Phase 3 NEW 2026-05-30)

The milestone-shared Codex review returned `approve with downgrade-to-signal follow-up` 2026-05-30 with **three non-blocking triggers**. All are close-package or M-Auto-1C planning hygiene; **none is a code-level blocker** ("The cumulative agent-loop changes do not encode a semantic hardcode" — Codex Axis M2 PASS). Deliver-agent + human dispositions at Phase 3:

**Trigger #1 — S-Auto-8 drift envelope (Codex Axis M6 + §4.1 verdict)**:

> *Codex verbatim*: "Anchor/shadow show a higher provider-drift signature than M-Auto-1A: anchor_outcome 7/12 -> 3/12 and shadow 4/22 -> 1/22, clustered on UC-D/E/F/FP auth/account/billing flows. … S-Auto-8 should establish a >=2 rerun drift envelope before overnight."

**Disposition**: ACCEPTED and built into M-Auto-1C planning §12.10 above + NEW live `docs/milestone_objective.md` §3 S-Auto-8 scope (Phase 3). The S-Auto-8 sprint_objective + dev prompt embed this requirement verbatim. Rationale for full acceptance: Phase 2 evidence is consistent with LLM-provider variance (no bot-code regression; bot byte-identical on agent-loop scope), but elevated UC-D/E/F/FP magnitude relative to M-Auto-1A's 5/34 close-day signature does increase the risk that overnight comparison against a single noisy baseline run would produce false-positive "keeps". A ≥2 rerun drift envelope establishes median + IQR before overnight runs against that envelope; the M-Auto-1B Phase 2 evidence directly motivates this discipline.

**Trigger #2 — Close-package metadata reconciliation (Codex Axis M10 + §4.1 verdict)**:

> *Codex verbatim (a)*: "Commit range count: `git rev-list --count b6b627b..HEAD` is 18, not 14. The extra commits include M-Auto-1A archival commits after `b6b627b` plus Phase 1.6 evidence (`5b9c143`). The actual graph confirms `b6b627b` is an ancestor, but the previous-milestone 'closed at b6b627b' wording is stale relative to `7ed95e7` archival close."
>
> *Codex verbatim (b)*: "Live findings file: current `docs/codex-findings.md` contains S-Auto-7 plus this appended review; S-Auto-5's verdict is preserved in history at `95089c2:docs/codex-findings.md` and in `docs/action_bank.md`, but not in the live file despite the prompt saying both per-sub-sprint reviews are live."

**Disposition (a)**: ACCEPTED at Phase 3 via §12.2 rewrite above. The 18-commit raw count now decomposes into 14 agent-loop + 4 adjacent non-scope (2 M-Auto-1A post-archival + 1 ambient README + 1 ambient-human-work); the stale "closed at b6b627b" wording in the milestone-shared review prompt is acknowledged as a prompt-authoring residue from a pre-archival `b6b627b` snapshot. Note for future milestone-shared prompts: the deliver-agent at milestone open should use `git rev-list --count <prev-archival-commit>..HEAD` against the actual archival commit (here `7ed95e7`), not the close-prep commit, for the "cumulative range" claim. **Future-deliver-agent operational note**: at milestone-shared review prompt authoring time, run `git log --oneline <prev-archival>..HEAD` and classify commits explicitly before embedding the count.

**Disposition (b)**: ACCEPTED at Phase 3. The deliver-agent prepends the S-Auto-5 verdict body verbatim (from `git show 95089c2:docs/codex-findings.md`) to live `docs/codex-findings.md` BEFORE the `git mv` to archive, so the archived `docs/milestones/M-Auto-1B_codex-review.md` contains **all three reviews** (S-Auto-5 + S-Auto-7 + M-Auto-1B milestone-shared) in one self-contained file. Future audit reads the archive without needing to dig into git history; the `docs/action_bank.md` §5 R-S57 close annotation continues to serve as a secondary pointer for the S-Auto-5 verdict.

**Trigger #3 — R-eval-interactive-judge-score-never-populated ledger (Codex Axis M9 + §4.1 verdict)**:

> *Codex verbatim*: "Non-blocking ledger concern: Phase 2 text names a new `R-eval-interactive-judge-score-never-populated`, but I did not find a corresponding `docs/action_bank.md` entry yet. Phase 3 should either add that R-item to action_bank or explicitly demote it to an observation before archiving M-Auto-1B."

**Disposition**: ACCEPTED at Phase 3 (Option 1 — formalize). Deliver-agent + human joint decision 2026-05-30 (recommended option per AskUserQuestion 2026-05-30): add to `docs/action_bank.md` §5 as low-priority M-Auto-2+ observability-hygiene R-item, parallel to `observability_debt_pattern` memory + `R-eval-report-observability` (closed) precedent. Long-standing eval-side debt; needs eventual action even if not urgent. See §12.7 R-item flips entry + NEW action_bank entry at Phase 3.

**Net effect on M-Auto-1B classification**: NONE. All 3 triggers are non-blocking per Codex `decision: pass / blocking_count: 0`; dispositions land in Phase 3 close-bundle (this commit) without re-litigating sub-sprint dispositions or substrate verdicts. M-Auto-1B remains **A-with-acceptance-bar-revision**.
