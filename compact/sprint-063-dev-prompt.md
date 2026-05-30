# Sprint 063 / S-Auto-8 / M-Auto-1C — Dev Session Prompt

You are the dev agent (Claude Code) for **Sprint 063 / S-Auto-8 / M-Auto-1C — Auto-Evolution Calibration Continuation, sub-sprint 2 of 2**. Your goal in one sentence: **exercise the now-validated substrate (post-OQ-S61.1 + OQ-S62.1 + OQ-S62.2 fixes from S-Auto-7.2) end-to-end via first overnight batch + first human review of kept candidates + first cherry-pick to main** — retiring the 5 M-Auto-1B deferred hard gates inherited by M-Auto-1C.

This prompt is **self-contained per `iteration_governance.md` §9 invariant**. You do NOT need to read any repo doc beyond `AGENTS.md` (auto-loaded via constitution chain) + this prompt to execute the sub-sprint. Specific code anchors are named below where you need to read code.

## ⚠️ CRITICAL: OQ-S62.3 operational workaround — READ FIRST

**The Claude Code bash-tool harness sandbox SIGKILLs long-running Python orchestrators mid-Step 7 eval_runner.** This was surfaced as OQ-S62.3 at S-Auto-7.2 (handoff §6.3). It is NOT a code defect — the substrate is already fixed. It IS an environment-level limitation.

**You MUST run the smoke iter + overnight batch via raw shell / screen / tmux / `nohup`, NOT via Claude Code's Bash tool with long-running invocation.**

**Recommended invocation pattern** (run in a regular Terminal / iTerm / screen / tmux session OUTSIDE Claude Code):

```bash
# For smoke iter (1 iteration):
nohup python -m autoloop run --experiments 1 > autoloop-smoke.log 2>&1 &

# For overnight batch (10-20 iterations):
nohup python -m autoloop run --experiments 15 > autoloop-overnight.log 2>&1 &

# Monitor progress:
tail -f autoloop-overnight.log
ls -la autoloop/results/experiments.jsonl
wc -l autoloop/results/experiments.jsonl
```

If you attempt the smoke iter or overnight via Claude Code's Bash tool with a long-running invocation, it will SIGKILL the orchestrator mid-Step 7 just like S-Auto-7.2 attempts and the `experiments.jsonl` row write will NOT capture. The substrate is fixed; only the harness execution environment needs the workaround.

For non-long-running setup commands (file edits, git status, pytest sanity checks), Claude Code's Bash tool is fine.

## Read order (minimal)

1. `AGENTS.md` is auto-loaded by Claude Code on session start; the constitution chain `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` loads transitively. **Do NOT manually read** these.
2. This prompt.
3. **Code anchors** (read on demand during work):
   - `autoloop/autoloop/loop.py` (the 14-step state machine; understand Step 6 Spring spawn + Step 7 eval_runner + Step 9 tier_evaluator).
   - `autoloop/autoloop/scoring/eval_runner.py` (Step 7 subprocess invocation; understand the auto-timestamped output consumption + symlink staging from S-Auto-7 fix).
   - `autoloop/autoloop/scoring/tier_evaluator.py` (Step 9 four-tier lexicographic verdict).
   - `autoloop/autoloop/sandbox/applier.py` (Step 6.4 git branch + commit + Step 6.6 mvn spawn + Step 6.7 actuator probe; `_git_create_branch` is now idempotent post-S-Auto-7.2 OQ-S62.2 fix; `_spawn_spring` dropped `-am` post-S-Auto-7.2 OQ-S61.1 fix).
   - `autoloop/autoloop/sandbox/content_validator.py` (Step 2.5 cv with new `length_overflow_absolute_ceiling: 1000` knob from S-Auto-7.2 OQ-S62.1 fix).
   - `autoloop/config.yaml` (review fitness.baseline_dir, fitness.scoring_code_baseline_sha, anti_hardcode, content_validator blocks).
   - `eval_interactive/case_specs/bad_cases/_manifest.md` (M-Auto-1B close section + per-case lifecycle entries).
   - `docs/action_bank.md` §5 R-S58 entry (the propose-distribution scan target).
4. **Do NOT read**: `docs/sprint_objective.md` (canonical contract; this prompt is its self-contained executable view), `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/sprints/sprint-062-*` (S-Auto-7.2 archives; relevant context embedded below).

## Sub-sprint contract (embedded verbatim from `docs/sprint_objective.md`)

### Class

- **Layer (primary)**: `eval_spec` (consumes per-iteration fitness verdict sequence on the now-validated substrate; cherry-pick decision via AskUserQuestion + §5.6 manual review).
- **§7 stanza**: **REQUIRED** (semantic-touching via cherry-pick if it lands; see §"§7 stanza" below).
- **Codex review plan**: milestone-shared (default) at M-Auto-1C close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint per §4.3 trigger #2 pre-milestone-close).

### Goal

Exercise the now-validated substrate end-to-end via first overnight batch + first human review + first cherry-pick. Retire the 5 M-Auto-1B deferred hard gates inherited by M-Auto-1C: live iter end-to-end through Step 9 + drift envelope + first overnight + first human review + first cherry-pick. Establish baseline drift envelope BEFORE overnight per Codex M-Auto-1B Axis M6 trigger #1 acceptance. Finalize R-S58 disposition based on overnight propose-distribution scan.

### Inherited context from S-Auto-7.2 close 2026-05-30

Three substrate fixes LANDED:
- **OQ-S61.1**: `applier.py:370-378` mvn `-am` removal; Spring READY in 40s on direct mvn spawn.
- **OQ-S62.1**: `content_validator.py` rule 3 length_overflow rewritten as `max(ratio*before, ceiling)` with new `length_overflow_absolute_ceiling: 1000` knob; short-before fields gaining coherent expansion no longer rejected.
- **OQ-S62.2**: `applier.py` `_git_create_branch` idempotent on stale-branch collision via new `_branch_exists` helper.

Codex per-sub-sprint review at S-Auto-7.2 close (2026-05-30): `decision: pass / blocking_count: 0 / approve with downgrade-to-signal follow-up` — 9/9 axes PASS or PASS-with-CONCERN.

OQ-S62.3 (harness sandbox kill mid-Step 7) is the carry-over — operational workaround above.

### Pre-overnight readiness items (inherited from S-Auto-7.2)

1. **≥2 baseline rerun drift envelope** (Codex M-Auto-1B Axis M6 trigger #1 acceptance): the elevated UC-D/E/F/FP LLM-provider drift signature observed at M-Auto-1B Phase 2 (anchor_outcome 7/12 → 3/12 + shadow 4/22 → 1/22; 7/34 = 20.6% vs M-Auto-1A close 5/34 = 14.7%) requires establishing per-suite case_passed median + IQR drift envelope BEFORE overnight kick-off. If baseline-vs-baseline drift exceeds 10/34 cases (~30%; 2× the M-Auto-1A close-day signature), HALT + surface to deliver-agent + human.
2. **OQ-S62.3 operational workaround**: run autoloop via raw shell (NOT Claude Code bash-tool). See top of this prompt.
3. **Stale autoloop/exp-* branch cleanup** (deferred per deliver-agent + human joint decision 2026-05-30): 8 stale exp-* branches (autoloop/exp-2, exp-6, exp-7, exp-8, exp-10, exp-11, exp-12, exp-13) add branch-listing noise but don't block (OQ-S62.2 idempotency means S-Auto-8 targets exp-14 cleanly regardless). Recommended cleanup at S-Auto-8 open: surface to human via AskUserQuestion + `git branch -D autoloop/exp-{2,6,7,8,10,11,12,13}` if authorized. Destructive; requires explicit human authorization.

### Bad-case cherry-pick reference signals from M-Auto-1B Phase 2

These are REFERENCE SIGNALS for manual review orientation, NOT pre-committed cherry-picks. The meta-agent owns proposal authorship; the cherry-pick decision is via AskUserQuestion on the actual overnight kept-candidate slate.

- **cs001_uc_c_mechanical_template_escalate**: candidate Skill YAML edit on `resolve_faq_grounded_answer.yaml` procedure / critical_steps[*].desc to gate `goal_impossible` terminal-state on policy-mandated `search_knowledge` completion.
- **wmkb_uc_a_trader_flag_secondary_uc_h**: candidate Skill YAML edit on `resolve_intake_collect_and_handover.yaml` critical_steps[*].desc to gate handover on intake completion.

### Scope (6 steps — execute in order)

1. **Pre-overnight readiness** (~2-4 hours):
   - OQ-S62.3 workaround prep: ensure you have a shell environment outside Claude Code bash-tool (Terminal / iTerm / screen / tmux).
   - Stale-branch cleanup option: surface to human via AskUserQuestion. If authorized, run `git branch -D autoloop/exp-{2,6,7,8,10,11,12,13}`. Record decision in handoff §0.
   - Foreground backend status: verify `:8080` is running (`curl http://localhost:8080/actuator/health`). If down, restart manually OR run `python -m autoloop preflight --auto-reboot`.
   - **Smoke iter end-to-end via raw shell** (this captures M-Auto-1B's Goal #3 measurement deferred via OQ-S62.3):
     ```bash
     # Outside Claude Code:
     nohup python -m autoloop run --experiments 1 > autoloop-smoke.log 2>&1 &
     # Wait ~15 min, then verify:
     tail -50 autoloop-smoke.log
     wc -l autoloop/results/experiments.jsonl
     jq -r '.verdict' autoloop/results/experiments.jsonl | tail -1
     ```
     - Verify `experiments.jsonl` has the new row + `tier_evaluator_verdict` non-degenerate (Layer 0-4 all non-null).
     - Record per-iter elapsed time.

2. **Pre-batch baseline drift envelope (≥2 rerun)**:
   - Run TWO OR MORE baseline reruns of the 3 v1 suites:
     ```bash
     cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1
     cd eval_interactive && uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4
     cd eval_interactive && uv run eval-interactive run --path case_specs_shadow/ --parallel 4
     # Repeat ≥1 more time per suite
     ```
   - For each suite: record case_passed count + median + IQR + bidirectional drift count vs prior runs.
   - If baseline-vs-baseline drift exceeds 10/34 cases across all 3 suites, STOP-and-surface to deliver-agent + human via AskUserQuestion BEFORE proceeding to overnight.

3. **Overnight batch** (outside Claude Code via raw shell):
   ```bash
   nohup python -m autoloop run --experiments 15 > autoloop-overnight.log 2>&1 &
   # Monitor in another terminal:
   tail -f autoloop-overnight.log
   watch -n 30 'wc -l autoloop/results/experiments.jsonl'
   ```
   - Target: 10-20 iterations; final count ≥10 is the close gate.
   - Budget: 6-8h.
   - If total errors >50% within first 5 iters, halt + surface.

4. **Propose-distribution scan for R-S58 evidence** (after overnight closes):
   - Scan per-iteration `propose_output.txt` or equivalent for U+200B / Unicode `category() == "Cf"` format characters:
     ```python
     # Run via raw shell or short script:
     import unicodedata
     import json
     count = 0
     with open("autoloop/results/experiments.jsonl") as f:
         for line in f:
             rec = json.loads(line)
             text = rec.get("propose_output", "")  # exact field name TBD; explore
             for ch in text:
                 if unicodedata.category(ch) == "Cf":
                     count += 1
                     break
     print(f"iters with Cf chars: {count} / 15")
     ```
   - Record per-iter occurrence count + frequency rate.
   - Use evidence for R-S58 disposition recommendation in handoff §5.

5. **§5.6 manual review of kept candidates** (morning after overnight; deliver-agent + human joint session):
   - List kept candidates: `jq -r '. | select(.verdict.decision == "keep") | .experiment_id' autoloop/results/experiments.jsonl`.
   - For each kept candidate, open `autoloop/results/runs/exp-<N>/` + corresponding `eval_interactive/results/<run-id>/`.
   - Read per-turn traces of cs001 + wmkb + other bad_cases + anchor_outcome runs.
   - Joint judgment per candidate: PASS / FAIL / IMPROVING / borderline-§5.3 / discard.
   - **Reference signals**: cs001 (Skill YAML on `resolve_faq_grounded_answer.yaml`) + wmkb (Skill YAML on `resolve_intake_collect_and_handover.yaml`) — orientation only.

6. **AskUserQuestion cherry-pick decision + apply Hybrid + close**:
   - Surface candidate slate to human via AskUserQuestion (each eligible candidate: target_skill, target_field, edit_summary, programmatic verdict, manual review verdict, recommendation).
   - Human selects EXACTLY ONE candidate OR 0 (with justification).
   - If 1 candidate selected:
     ```bash
     python -m autoloop apply --experiment exp-<N>  # Hybrid mode: cherry-pick + emit baseline patch + NO auto-commit
     git status
     git add server/src/main/resources/skills/<file>.yaml
     # Plus possibly autoloop/config.yaml if baseline_dir advances
     git commit -m "Sprint 063 / S-Auto-8 / M-Auto-1C — apply exp-<N> to main"
     ```
   - Update `autoloop/config.yaml` `fitness.baseline_dir` to advance past the cherry-pick commit (if any).
   - Finalize R-S58 disposition based on §4 scan evidence.
   - Author `docs/sprints/sprint-063-handoff.md`.

### Hard fences / STOP conditions

**Hard fences** (M-Auto-1C §6 17+2 list; fence #18 + #19 envelopes FINALIZED at S-Auto-7.2 close):

- **No edits** to: `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-062-*`, prior milestone archives under `docs/milestones/`.
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at hash `22548e20…`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,gaming}.py`, `autoloop/autoloop/sandbox/applier.py` (fence #18 FINALIZED), `autoloop/autoloop/sandbox/content_validator.py` (fence #19 FINALIZED), `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py`, `autoloop/autoloop/cli.py`.
- **Skill YAML cherry-pick ALLOWED EXACTLY ONCE** during S-Auto-8 — for ONE human-approved kept candidate via apply Hybrid mode.
- **No new Tier-0 invariant**.
- **No `git add -A`** — stage explicitly.

**STOP-and-surface conditions** (use AskUserQuestion):

- Baseline drift envelope >10/34 cases.
- Overnight halts before iter 5 with errors >50%.
- Smoke iter crashes through a NEW unobserved path.
- Overnight produces ZERO kept candidates → meta-agent prompt issue suspected.
- Cherry-pick candidate touches §1.7 borderline → upgrade to per-sub-sprint Codex BEFORE commit.
- Manual review surfaces a NEW failure shape (previously-PASSing case now FAILing) NOT explained by cherry-pick OR provider drift → investigate.

### Test / eval requirements

- Smoke iter via raw shell: `experiments.jsonl` row + non-degenerate `tier_evaluator_verdict`.
- Baseline rerun ≥2: drift envelope per-suite median + IQR.
- Overnight ≥10 iters.
- autoloop pytest: ≥ 266 PASS (baseline from S-Auto-7.2 close).
- eval_interactive: `486 passed, 3 failed` UNCHANGED.
- 17-fixture detector: 31 PASS UNCHANGED.
- scoring SHA: `22548e20…b46ea045…188a9` REASSERTED.
- Java baseline UNCHANGED.

### §7 stanza (REQUIRED — semantic-touching via cherry-pick)

- **Target failure layer**: `eval_spec` (manual review + cherry-pick decision); potentially `semantic_planner` if cherry-pick edit is on Skill YAML procedure / critical_steps[*].desc.
- **Tier-0 invariant**: None added. Skill YAML cherry-pick on LLM-soft narrative fields only.
- **Semantic hardcode**: None introduced by S-Auto-8 dev. Cherry-pick candidate goes through sandbox + anti-hardcode detector at propose; manual review validates the no-keyword-routing guarantee against per-turn traces.
- **Generalization coverage**: target = failure shape addressed by cherry-pick (cs001 / wmkb / other). Neighbor = related UC cases. Negative = unaffected UC. Shadow = 22 shadow cases at M-Auto-1C close.

### Codex review plan (per §4.3)

**Default**: milestone-shared at M-Auto-1C close. Deliver-agent + human dispatch the M-Auto-1C milestone-shared review prompt at M-Auto-1C close-bundle commit.

**Trigger condition** (dev STOP-and-surfaces if fires):

- Cherry-pick candidate touches §1.7 borderline → upgrade to per-sub-sprint Codex pre-milestone-close per §4.3 trigger #2.

### Handoff requirements

`docs/sprints/sprint-063-handoff.md` author at sub-sprint close. Mandatory sections §0-§11 per `docs/sprint_objective.md` "Handoff requirements" section (embedded summary):

1. §0 Sub-sprint summary (goal/scope/commits/test counts/smoke iter/baseline envelope/overnight iter count/cherry-pick decision/R-S58 disposition).
2. §1 Cumulative changes (per-commit + per-file LOC).
3. §2 OQ-S62.3 workaround verification (invocation method + smoke iter `experiments.jsonl` capture evidence; this RETIRES M-Auto-1B Goal #3).
4. §3 Pre-batch baseline drift envelope.
5. §4 Overnight batch evidence.
6. §5 R-S58 propose-distribution scan evidence + final disposition recommendation.
7. §6 §5.6 manual review per-candidate classifications.
8. §7 Cherry-pick decision via AskUserQuestion + apply Hybrid evidence.
9. §8 OQs surfaced (if any).
10. §9 STOP-and-surface log (if any).
11. §10 M-Auto-1C close readiness checklist (13 hard gates from §5 acceptance bar + Stage-2 entry decision recommendation direction for M-Auto-2+ planning).

### Commit discipline

- Multi-commit pattern acceptable.
- Commit message format: `Sprint 063 / S-Auto-8 / M-Auto-1C — <description>`.
- Standard deliver-agent footer.
- No `git add -A`.
- Stage explicitly per-commit (baseline rerun results separately from overnight separately from cherry-pick separately from handoff).

## Self-check checklist (MUST verify before claiming done)

- [ ] OQ-S62.3 workaround used (raw shell / screen / tmux outside Claude Code bash-tool); smoke iter captured `experiments.jsonl` row + non-degenerate `tier_evaluator_verdict`.
- [ ] Pre-batch baseline rerun ≥2 completed; drift envelope recorded.
- [ ] Overnight ≥10 iterations completed; per-iter verdicts serialized.
- [ ] R-S58 propose-distribution scan completed.
- [ ] §5.6 manual review of kept candidates jointly conducted.
- [ ] AskUserQuestion cherry-pick decision recorded.
- [ ] (If cherry-pick) apply Hybrid + human manual commit successful; `baseline_dir` advances.
- [ ] autoloop pytest ≥ 266 PASS.
- [ ] eval_interactive baseline UNCHANGED.
- [ ] 17-fixture detector sweep PASS UNCHANGED.
- [ ] scoring SHA reasserted.
- [ ] Hard-fence diff cumulative against M-Auto-1C §6 gated paths returns empty (or only conditional Skill YAML if cherry-pick).
- [ ] Handoff §0-§11 sections all filled.
- [ ] M-Auto-1C close readiness: 13 hard gates tick-off; Stage-2 entry direction documented.
- [ ] Stale autoloop/exp-* cleanup decision recorded in §0 (deferred OR executed).

When all self-check items are complete, this sub-sprint is ready for deliver-agent + human review. Deliver-agent will dispatch M-Auto-1C close-bundle (milestone-shared Codex review prompt + close artefacts) after S-Auto-8 sub-sprint close evaluation.
