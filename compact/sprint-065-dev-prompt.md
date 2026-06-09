# Sprint 065 / S-Auto-10 / M-Auto-2 — Dev Implementation Prompt

You are the **dev agent (Claude Code)** for **Sprint 065 / S-Auto-10 / M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick, sub-sprint 2 of 2-3**. Your one-sentence goal: **exercise the now-reliably-executable substrate end-to-end via the first overnight batch + first §5.6 manual review + first cherry-pick decision via AskUserQuestion** (retire M-Auto-1C §12.4 deferred hard gates #2-#4: first overnight ≥10 iter + first human review + first cherry-pick).

This prompt is **self-contained per `iteration_governance.md` §9 invariant**. You do NOT need to read any repo doc beyond `AGENTS.md` (auto-loaded via the constitution chain) + this prompt. Code anchors are referenced inline where needed.

---

## ⚠️ CRITICAL operational constraints (read FIRST before any action)

These constraints are human-locked and inherited from M-Auto-2 framing 2026-05-30 + S-Auto-9 substrate state 2026-05-31:

1. **Local-Mac ONLY**. M-Auto-2 stays on the developer's local Mac. **DO NOT pursue cloud / remote-server framing**. The S-Auto-9 fence #20 applier.py fixes have resolved the local-Mac substrate brittleness; cloud framing is not needed. If you find yourself reaching for cloud / remote server, STOP-and-surface to the human via AskUserQuestion.

2. **Substrate is now reliably executable post-S-Auto-9** (commit `e342d89`). The fence #20 applier.py fixes are:
   - **`_spawn_spring`** now uses `start_new_session=True` → mvn + JVM occupy their own session / process group; `_terminate_process`'s `killpg` no longer suicides the orchestrator (OQ-S62.3 RESOLVED).
   - **`_spawn_spring`** routes mvn stdout to a per-port log file `autoloop/results/spring-boot-<port>.log` → no 64KB pipe-buffer deadlock during eval phase (OQ-S64.1 RESOLVED).
   - **`_probe_url_is_up`** uses `httpx.Client(trust_env=False)` → localhost health probe bypasses macOS system proxy (`127.0.0.1:7890`) → no spurious 120s `SpringStartupTimeoutError` (OQ-S64.2 RESOLVED).
   - Smoke iter `exp-18` confirmed Step 9 reach in ~71s with non-null 5-layer `tier_evaluator_verdict`.

3. **OQ-S64.3 observation**: full iterations now run in ~71s end-to-end (not the originally-planned ~12-15 min). A 15-iter batch may complete in ~15-20 min wall time. The 6-8h budget is **demoted to observation**; the M-Auto-2 §5 hard gate is **≥10 iterations completed end-to-end** regardless of wall time. Plan accordingly.

4. **Fast-iteration STOP-and-surface authorization** (per S-Auto-7.2 + S-Auto-9 precedent + user direction "可以我review后快速放宽通过"): if you surface a substrate brittleness mid-overnight that's NOT explained by OQ-S62.3 / OQ-S64.1 / OQ-S64.2, STOP via AskUserQuestion + recommend a narrow fix; human will authorize quickly if it's structurally similar to the S-Auto-9 pattern. Do NOT silently route around new brittlenesses.

5. **Fence #20 surface is FINALIZED** at S-Auto-9 close. You **MUST NOT** edit `autoloop/autoloop/sandbox/applier.py` (or any other M-Auto-2 §6 hard-fenced surface). The cherry-pick mechanism (if invoked) modifies Skill YAML LLM-soft fields ONLY (fence #3).

6. **Inherited Codex M-Auto-1C downgrade-to-signal triggers** (must address during execution + handoff):
   - **Trigger #2 — Drift-envelope metric hygiene**: at handoff §X baseline drift envelope, MUST explicitly cite loader-counted `case_passed` (via `composite.py` + `baseline_loader.load`). Do NOT use the CLI `Passed: N` headline (structurally `0` when `judge_score` uniform 0.0 per `R-eval-interactive-judge-score-never-populated`).
   - **Trigger #3 — cv ceiling calibration discipline**: do NOT bump `autoloop/config.yaml` `length_overflow_absolute_ceiling` (currently 1200) on a single anecdote. Accumulate ≥5-10 propose-stage data points across the overnight batches BEFORE considering further adjustment. Record PASS / FAIL bands from real proposals.
   - **Trigger #4 — Prompt/template hygiene**: stale references corrected in this prompt — use `yaml.safe_load(Path('config.yaml').read_text())` NOT `from autoloop.config import load`; `gaming.py` is at `autoloop/autoloop/scoring/gaming.py` NOT `autoloop/autoloop/sandbox/gaming.py`. Verify no stale references re-appear in your handoff.

7. **NO `git add -A`** — stage explicitly per commit. Each commit stages only its named scope files.

---

## Role identity

You are dev agent for **Sprint 065 / S-Auto-10 / M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick, sub-sprint 2 of 2-3**. Layer = `eval_spec` (per-iteration fitness verdict consumption on now-reliably-executable substrate; §5.6 manual review + cherry-pick are eval-side acceptance bars; semantic-touching via cherry-pick on Skill YAML LLM-soft fields if it lands).

§7 stanza: **REQUIRED** (semantic-touching via cherry-pick). The stanza is embedded below under "§7 stanza".

Codex review plan: **milestone-shared (default)** at M-Auto-2 close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint per §4.3 trigger #2 pre-milestone-close).

---

## Read order (minimal)

1. `AGENTS.md` is auto-loaded by Claude Code on session start; the constitution chain `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` loads transitively. **Do NOT manually read** these.
2. This prompt.
3. **Code anchors** (read on demand during work):
   - `autoloop/autoloop/sandbox/applier.py` (read-only audit of fence #20 surface; do NOT edit). Functions of interest: `_spawn_spring` (S-Auto-9 fixes for OQ-S62.3 + OQ-S64.1) + `_probe_url_is_up` (S-Auto-9 fix for OQ-S64.2) + `apply_cherry_pick` (or equivalent; the cherry-pick entry point).
   - `autoloop/autoloop/cli.py` (read-only; understand `autoloop run --experiments N --auto-reboot` + `autoloop apply --experiment exp-<N>` Hybrid mode flags).
   - `autoloop/config.yaml` (read-only; verify current `length_overflow_absolute_ceiling: 1200` + `fitness.baseline_dir` current pointer; do NOT edit unless cherry-pick lands AND baseline_dir advancement is part of the apply Hybrid output).
   - `scripts/sprint-064-step4-smoke-iter.sh` (S-Auto-9 proven launcher pattern; you MAY copy / adapt as `scripts/sprint-065-overnight.sh` if useful for the overnight batch; OR launch via raw shell `nohup python -u -m autoloop run --experiments 15 --auto-reboot &` per OQ-zsh-BG_NICE NI=0 hygiene).
   - `eval_interactive/eval_interactive/composite.py` + `eval_interactive/eval_interactive/baseline_loader.py` (read-only; understand loader-counted `case_passed` semantics per Codex M-Auto-1C trigger #2).
   - `eval_interactive/case_specs/bad_cases/` + `eval_interactive/case_specs/anchor_outcome/` + `eval_interactive/case_specs_shadow/` (read-only; the 3 suites for pre-batch baseline rerun + §5.6 manual review pool).
   - `autoloop/results/runs/exp-*/` + `autoloop/results/experiments.jsonl` + `autoloop/results/iterations.sqlite` + `autoloop/results/lessons.md` (read after overnight batch executes; per-iter artefacts for §5.6 manual review + propose-distribution scan).
   - `server/src/main/resources/skills/*.yaml` (read-only audit; cherry-pick edit target IF it lands — Skill YAML LLM-soft fields procedure / grounding_instruction / escalation_policy / critical_steps[*].desc ONLY per fence #3 + #14).

---

## Embedded sub-sprint contract

### Class

- **Layer (primary)**: `eval_spec`. Per §3.2 question 6, the eval CaseSpec / judge ask the system to do something it can / should do — the cherry-pick mechanism consumes the per-iteration fitness verdict sequence; the §5.6 manual review + cherry-pick selection ARE the eval-side acceptance bars.
- **§7 stanza**: REQUIRED (semantic-touching via cherry-pick on Skill YAML LLM-soft fields if it lands).
- **Codex review plan**: milestone-shared (default) at M-Auto-2 close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint per §4.3 trigger #2 pre-milestone-close).
- **Sub-sprint position in milestone**: 2nd of 2-3 (S-Auto-9 OQ-S62.3 diagnostic + resolution CLOSED 2026-05-31 PASS → **S-Auto-10 first overnight + first cherry-pick (THIS)** → optional S-Auto-11 fix-iteration buffer → M-Auto-2 close).

### Goal

Exercise the now-reliably-executable substrate end-to-end via first overnight batch + first §5.6 manual review of kept candidates + first cherry-pick decision via AskUserQuestion. Retire M-Auto-1C §12.4 deferred hard gates #2-#4 (first overnight ≥10 iter + first review + first cherry-pick). Surface evidence for R-S58 reopen check (propose-distribution Cf scan) + M-Auto-2 close + post-M-Auto-2 planning (Stage-2 unlock direction).

**Acceptance**: ≥10 iterations completed end-to-end; per-iter rows in `autoloop/results/experiments.jsonl` with non-null `verdict.layer_results`; propose-distribution scan complete; cherry-pick decision via AskUserQuestion (exactly ONE or 0 with explicit justification); if cherry-pick lands, human commit visible on `auto-loop-branch`.

### Scope (6 steps; re-scoped per OQ-S64.3 ~71s/iter)

#### Step 1 — Pre-flight env check + pre-batch baseline rerun ≥2 (~half-day)

- Verify foreground :8080 backend healthy. If not running, launch via `mvn -pl server spring-boot:run` from repo root in a separate terminal OR rely on `--auto-reboot` flag when launching autoloop.
- Verify substrate health via smoke iter:
  ```bash
  cd <repo-root>
  scripts/sprint-064-step4-smoke-iter.sh 1
  # OR equivalent: python -m autoloop run --experiments 1 --auto-reboot
  ```
  Expected: 1 iter reaches Step 9 (~71s wall time); writes a row to `autoloop/results/experiments.jsonl` with non-null `verdict.layer_results`. If NOT, STOP-and-surface (S-Auto-9 fixes may have regressed; investigate before proceeding).
- Pre-batch baseline rerun ≥2 per suite (inherited Codex M-Auto-1B Axis M6 acceptance + M-Auto-1C close-day precedent):
  ```bash
  cd eval_interactive
  # 2 reruns each suite
  uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1     # 12 cases; ~10 min/run
  uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1
  uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4 # 12 cases; ~3 min/run
  uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4
  uv run eval-interactive run --path case_specs_shadow/ --parallel 4         # 22 cases; ~5 min/run
  uv run eval-interactive run --path case_specs_shadow/ --parallel 4
  ```
- Record per-suite loader-counted `case_passed` envelope (per Codex M-Auto-1C trigger #2):
  - Loader-counted `case_passed` is read via:
    ```python
    from eval_interactive.baseline_loader import load
    from eval_interactive.composite import …  # consult actual canonical API
    ```
    OR equivalent CLI reporting that surfaces the loader-counted `case_passed`. Do NOT cite the CLI `Passed: N` headline (structurally `0` when `judge_score` uniform 0.0 per `R-eval-interactive-judge-score-never-populated`).
- **Halt condition**: if drift exceeds 10/34 cases (~30%), STOP-and-surface to deliver-agent + human via AskUserQuestion. Likely LLM-provider variance beyond M-Auto-1A signature; possibly post-S-Auto-9-substrate change exposed new variance.

#### Step 2 — First overnight batch ≥10 iter (~30 min wall time at ~71s/iter)

- Launch via raw shell / Terminal / iTerm (NOT inside Claude Code Bash tool — the Bash tool's per-command timeout makes ≥10 iter batches inconvenient):
  ```bash
  # OQ-zsh-BG_NICE hygiene: launch from bash, not zsh, to keep NI=0
  # (per S-Auto-9 §6 OQ-zsh-BG_NICE observation)
  bash -c 'cd <repo-root> && nohup python -u -m autoloop run --experiments 15 --auto-reboot > /tmp/sprint-065-overnight.log 2>&1 &'
  echo $!  # capture PID for tracking
  ```
- Alternatively use the S-Auto-9 proven wrapper pattern: `scripts/sprint-064-step4-smoke-iter.sh 15` (adapts the per-iter signal-trap + foreground/detached launch).
- Monitor via `tail -F /tmp/sprint-065-overnight.log` in a separate terminal.
- Verify substrate doesn't OS-kill (S-Auto-9 fixes hold). The kill that sprint-063 misattributed to OS-level mechanisms is resolved; mvn + JVM now occupy their own session/pgrp.
- Capture per-iter:
  - Wall time (target ~71s; observation only — no hard gate on iter cost).
  - `verdict.layer_results` (5-layer structure; Layer 0 PASS prerequisite; Layers 1-4 may PASS / FAIL / short-circuit per tier_evaluator semantics).
  - Lessons compaction at iter 10 (K=10 trigger; verify `autoloop/results/lessons.md` populates).
- **Expected outcome**: ≥10 iter complete with non-null verdicts; if <10 due to substrate halt OR LLM-API halt, STOP-and-surface.

#### Step 3 — Optional second batch (CONDITIONAL; ~1-1.5h wall time at ~71s/iter for 30 iter)

If first batch completed cleanly AND you judge a second batch is useful (e.g., richer R-S58 propose-distribution; richer §5.6 candidate pool; lessons K=20 exercise), run:

```bash
bash -c 'cd <repo-root> && nohup python -u -m autoloop run --experiments 30 --auto-reboot > /tmp/sprint-065-overnight2.log 2>&1 &'
```

- Same monitoring pattern as Step 2.
- If second batch surfaces ANY new substrate brittleness (e.g., a previously-unobserved Spring spawn pathology; a new lessons compaction crash; an unexpected gaming check ERROR-severity hit), STOP-and-surface. Do NOT silently continue.
- If you judge second batch unnecessary (e.g., first batch already produced a clean cherry-pick candidate slate), skip and proceed to Step 4.

#### Step 4 — Propose-distribution scan for R-S58 reopen check (~half-day)

- Across all overnight iterations (Step 2 + optional Step 3), scan propose-stage artefacts for **Cf (zero-width / format-control) characters** in proposed Skill YAML content:
  ```bash
  # Inspect proposal artefacts (path layout may vary; consult autoloop docs OR walk autoloop/results/runs/exp-*/ to find the propose-stage record)
  python -c "
  import pathlib, unicodedata
  cf_count = 0
  cf_samples = []
  for proposal_path in pathlib.Path('autoloop/results/runs').rglob('proposal*.yaml'):
      content = proposal_path.read_text()
      for ch in content:
          if unicodedata.category(ch) == 'Cf':
              cf_count += 1
              cf_samples.append((str(proposal_path), repr(ch), unicodedata.name(ch, '<unnamed>')))
              break
  print(f'Cf observations: {cf_count} across {len(list(pathlib.Path(\"autoloop/results/runs\").rglob(\"proposal*.yaml\")))} proposals')
  for s in cf_samples[:5]:
      print(' -', s)
  "
  ```
  (Adjust path glob to match actual proposal artefact layout in `autoloop/results/runs/`.)
- **Expected**: 0 Cf observations across ALL propose-stage data points (consistent with M-Auto-1C S-Auto-8 baseline scan 0/13 historical experiments.jsonl rows).
- **If ≥1 Cf observation**: STOP-and-surface to deliver-agent + human. Re-promote R-S58 from CLOSED-AS-THEORETICAL-ONLY to ACTIVE per the reopen condition documented in `docs/action_bank.md` §5.2. Route to a Fix-D-style follow-on (potentially S-Auto-11 fix-iteration buffer if in-scope for M-Auto-2 close).

#### Step 5 — §5.6 manual review of kept candidates + cherry-pick decision (~1-2 days)

- Identify the kept-candidate slate: per-iteration verdicts with Layer 1-4 all PASS (Layer 0 PASS prerequisite). Discarded trajectories are NOT in the slate.
- Deliver-agent + human jointly read per-turn traces for each kept candidate using the §5.6 manual-review process:
  - Read `case_results[].per_turn_trace[]` per kept candidate.
  - Classify PASS / FAIL / IMPROVING / borderline-§5.3 per candidate.
  - Note: this is a **human-judgment gate**, not a programmatic gate. The programmatic Layer 1-4 PASS signal is necessary but not sufficient.
- **Bad-case reference signals (UNCHANGED from M-Auto-1C; hints to direct attention, NOT prescriptive targets)**:
  - `cs001_uc_c_mechanical_template_escalate` — resolve_faq_grounded_answer.yaml procedure / critical_steps[*].desc gating `goal_impossible` terminal-state on policy-mandated `search_knowledge` completion.
  - `wmkb_uc_a_trader_flag_secondary_uc_h` — resolve_intake_collect_and_handover.yaml critical_steps[*].desc gating handover on intake completion.
- Run AskUserQuestion at cherry-pick decision:
  ```
  AskUserQuestion(question="Cherry-pick decision: from the §5.6-reviewed kept-candidate slate, which candidate should land on auto-loop-branch via `autoloop apply` Hybrid mode?", options=[
    "Candidate <id-1>: <one-line description>",
    "Candidate <id-2>: <one-line description>",
    ...
    "0 cherry-pick — none of the kept candidates meet the §5.6 PASS bar; explicit justification: <reason>"
  ])
  ```
- **If cherry-pick lands**:
  - Run `python -m autoloop apply --experiment exp-<N>` in Hybrid mode (per OQ-S55.1: cherry-pick + emit baseline patch + NO auto-commit).
  - Verify the apply output: Skill YAML edit isolated to LLM-soft fields (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc); sandbox PASS; anti-hardcode PASS; gaming 0-ERROR.
  - Human manually commits with standard deliver-agent footer.
  - `config.fitness.baseline_dir` advances past the cherry-pick commit (verify via `git log autoloop/results/baselines/<dir>` or equivalent baseline-pointer mechanism).
- **If 0 cherry-pick lands**: handoff §6 records the explicit "no human-approved candidate" justification.

#### Step 6 — OQ ledger update + handoff + close-bundle prep (~half-day)

- Update `docs/sprints/sprint-065-handoff.md` per the §"Handoff requirements" below.
- Surface any new OQs (per-iter elapsed time observations, lessons compaction notes, propose-distribution observations beyond R-S58).
- Record M-Auto-2 close-readiness checklist: all 13 acceptance bar items addressed by S-Auto-10 contribution (S-Auto-9 retired #4, #5; S-Auto-10 retires #2, #6, #7, #8, #9; #1, #3 baseline gates verified throughout; #10 §5.6 manual review carried out at this sub-sprint; #11 shadow regression-safety verified; #12 R-S58 final disposition; #13 milestone-shared Codex deferred to M-Auto-2 close).
- Record R-S58 disposition: CLOSED-AS-THEORETICAL-ONLY confirmed OR re-promoted to ACTIVE.
- Surface Stage-2 entry decision direction for M-Auto-3+ planning.

### Hard fences (M-Auto-2 §6 17+3 list inherited post-§8.3 revision 2026-05-31)

- **No edits** to `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-064-*`, prior milestone archives under `docs/milestones/`.
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13; SHA-locked at `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,content_validator}.py` (fence #15 + #19 envelopes FINALIZED at M-Auto-1C close).
- **No edits** to `autoloop/autoloop/sandbox/applier.py` (fence #18 + fence #20 envelopes FINALIZED; S-Auto-9 closed the fence #20 envelope).
- **No edits** to `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py`, `autoloop/autoloop/cli.py`, `autoloop/config.yaml`.
- **Skill YAML edits** in `server/src/main/resources/skills/**`: writable EXACTLY ONCE via the cherry-pick mechanism per fence #3. Cherry-pick must be sandbox-validated, anti-hardcode PASS, gaming 0-ERROR, AND approved via AskUserQuestion. At most **1** cherry-pick at S-Auto-10 close.
- **No new Tier-0 invariant**.
- **No `git add -A`** — stage explicitly per commit.
- **No cloud / remote-server framing**.

### STOP-and-surface conditions

Dev pauses + asks deliver-agent + human via AskUserQuestion:

- Step 1 pre-flight smoke iter fails to reach Step 9 (S-Auto-9 fixes regressed).
- Step 1 pre-batch baseline drift envelope >10/34 cases.
- Step 2 first overnight halts before iteration 5 (>50% errors within first 5 iters).
- Step 2 first overnight surfaces substrate brittleness NOT explained by OQ-S62.3 / OQ-S64.1 / OQ-S64.2 (e.g., a previously-unobserved Spring spawn pathology surfaces).
- Step 3 optional second batch surfaces ANY new substrate brittleness.
- Step 4 propose-distribution scan surfaces ≥1 Cf observation.
- Step 5 cherry-pick candidate touches §1.7 forbidden-list borderline (proposal encodes visible-eval case text or trace-specific phrasing) → per §4.3 trigger #2 the cherry-pick triggers per-sub-sprint Codex pre-milestone-close; deliver-agent + human reconsider cherry-pick selection.
- Step 5 §5.6 manual review surfaces a NEW failure shape NOT in the cherry-pick scope (surface as new bad-case candidate for `bad_cases/_manifest.md` consideration at M-Auto-2 close).
- Hard-fence touch required beyond M-Auto-2 §6 17+3 envelope.
- Human cancels mid-sprint.

### Test / eval requirements

- **autoloop pytest baseline preserved**: `cd autoloop && uv run --extra dev pytest -q` returns ≥ 266 PASS (UNCHANGED from S-Auto-9 close). S-Auto-10 typically adds 0 new tests.
- **eval_interactive pytest baseline UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed`.
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns `31 passed`.
- **scoring_code_baseline_sha REASSERTED**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` returns `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`. To verify `_check_scoring_code_drift` is silent:
  ```python
  import yaml
  from pathlib import Path
  config = yaml.safe_load(Path('autoloop/config.yaml').read_text())
  # Then invoke _check_scoring_code_drift(config); expected empty list.
  ```
  (Adjust per actual canonical invocation pattern; the key is `yaml.safe_load(...)`, NOT `from autoloop.config import load` — there is NO `autoloop.config` module.)
- **Java baseline UNCHANGED**: zero-touch verified via `git diff --stat <S-Auto-9-close>..HEAD -- server/ eval/src/main/java/` returning empty.
- **Pre-batch baseline drift envelope**: ≥2 reruns per suite; per-suite loader-counted `case_passed` envelope cited (per Codex M-Auto-1C trigger #2); halt if drift >10/34 cases.
- **≥10 iterations completed end-to-end** at S-Auto-10 close (hard gate per M-Auto-2 §5 acceptance bar item 7).
- **At most 1 Skill YAML cherry-pick**: passes sandbox + anti-hardcode + gaming 0-ERROR + AskUserQuestion approval.

### §7 stanza (REQUIRED — semantic-touching via cherry-pick)

**Target failure layer**: `eval_spec` per `iteration_governance.md` §3.2 question 6 — the cherry-pick mechanism consumes the per-iteration fitness verdict sequence; the §5.6 manual review + cherry-pick selection ARE the eval-side acceptance bars. The cherry-pick edit itself, if it lands, is a Skill YAML LLM-soft field edit — semantic-touching but per fence #14 constrained to NOT encode case wording / user utterance / expected answer / case-status label.

**Tier-0 invariant**: this sub-sprint adds no Tier-0 invariant. C2/C3 candidates from M2 close remain DEFER unchanged. The cherry-pick edit (if any) does NOT introduce a Tier-0 invariant; it modifies a Skill YAML LLM-soft field within the existing runtime contract.

**Semantic hardcode**: no semantic hardcode introduced at the sub-sprint scope. The cherry-pick edit itself MUST pass the anti-hardcode detector (17-fixture sweep + propose-stage anti-hardcode check; 0-ERROR gating); the §5.6 manual review + Codex review (per §4.3 trigger #2 if borderline) verify no §1.7 forbidden-list red line crossed. If the cherry-pick is borderline, human declines via AskUserQuestion 0-cherry-pick option.

**Generalization coverage**: target = the cherry-pick's specific failure-shape. Neighbor = other UC variants of the same failure shape (typically observed within the overnight kept-candidate slate). Negative-control = bad cases the cherry-pick should NOT trigger (typically observed within the overnight discarded-trajectory slate). Shadow = held-out shadow cases (`case_specs_shadow/`); shadow drop ≤3% per M-Auto-2 §5 acceptance bar item 11. Final coverage matrix recorded in handoff §X if cherry-pick lands.

### Codex review plan (per §4.3)

**Default**: milestone-shared at M-Auto-2 close. Deliver-agent + human dispatch the M-Auto-2 milestone-shared review prompt at close-bundle commit (covers S-Auto-9 + S-Auto-10 + cherry-pick commit if any + optional S-Auto-11).

**Trigger conditions** (you STOP-and-surface if any fire):

- §4.3 trigger #1 (new Tier-0 candidate) DOES NOT fire (no Tier-0 invariant introduced).
- §4.3 trigger #2 (§1.7 forbidden-list red line) **CAN FIRE** if cherry-pick candidate borderline §5.3 (e.g., proposal narrowly encodes specific phrasing). In that case, upgrade S-Auto-10 to PER-SUB-SPRINT Codex per §4.3 trigger #2 pre-milestone-close BEFORE M-Auto-2 milestone-shared dispatches. Authorization follows S-Auto-7.2 + S-Auto-9 fast-iteration precedent.
- §4.3 trigger #3 (hard-fenced surface) DOES NOT fire (you MUST NOT edit fence-#20 surface OR any other hard-fenced surface beyond the fence #3 cherry-pick mechanism on Skill YAML).
- §4.3 trigger #4 (fix_required on prior sub-sprint) DOES NOT fire UNLESS S-Auto-9 Codex returned `fix_required` (expected: `approve` or `approve with downgrade-to-signal follow-up`).

### Handoff requirements

Author `docs/sprints/sprint-065-handoff.md` at sub-sprint close. Mandatory sections:

1. **§0 Sub-sprint summary**: Goal, scope, cumulative commit list, final test counts, overnight iter counts (first batch + optional second), §5.6 manual review summary, cherry-pick decision (1 or 0), R-S58 disposition, M-Auto-1C §12.4 deferred hard gates #2-#4 retirement confirmation.
2. **§1 Pre-flight + pre-batch baseline drift envelope**: foreground :8080 backend health + smoke iter confirmation + per-suite loader-counted `case_passed` envelope across ≥2 reruns per suite (explicit loader-counted citation; NOT CLI `Passed:N`).
3. **§2 First overnight batch**: launch + PID + per-iter elapsed time + per-iter verdict.layer_results summary + lessons compaction at K=10.
4. **§3 Optional second batch (if run)**: same evidence format.
5. **§4 Propose-distribution scan**: per-iter propose-stage Cf observation counts (expected 0; R-S58 reopen condition tracked).
6. **§5 §5.6 manual review**: kept-candidate slate + per-candidate PASS / FAIL / IMPROVING / borderline-§5.3 classification + cherry-pick decision rationale.
7. **§6 Cherry-pick apply (if any)**: AskUserQuestion record + apply Hybrid mode evidence + human commit hash + `config.fitness.baseline_dir` advancement + sandbox / anti-hardcode / gaming PASS evidence.
8. **§7 R-S58 disposition**: CLOSED-AS-THEORETICAL-ONLY confirmed OR re-promoted to ACTIVE; if re-promoted, Fix-D follow-on plan + S-Auto-11 in-scope evaluation.
9. **§8 OQs surfaced (if any)**: any new OQ-S65.x carry-over to S-Auto-11 OR M-Auto-3.
10. **§9 STOP-and-surface log (if any)**: timestamps + AskUserQuestion records.
11. **§10 M-Auto-2 close-readiness checklist (S-Auto-10 contribution)**: tick-off M-Auto-2 §5 acceptance bar items addressed.
12. **§11 Stage-2 entry decision direction**: based on cumulative M-Auto-1B + M-Auto-1C + M-Auto-2 evidence; direction for M-Auto-3+ planning.

### Commit discipline

- **Multi-commit pattern acceptable** (cherry-pick commit + handoff commit + optional result-artefact commit). Single-commit pattern preferred where possible.
- **Cherry-pick commit (if any)**: dedicated commit isolating the Skill YAML LLM-soft field edit ONLY.
- **Handoff commit**: handoff + auto-loop result artefacts (if checked in per existing convention; otherwise gitignored).
- **Commit message format**: `Sprint 065 / S-Auto-10 / M-Auto-2 — <commit description>` with bullet-point body summarizing per-commit scope.
- **Standard deliver-agent footer** at commit message end.
- **No `git add -A`** — stage explicitly.

---

## Operational anchors (how to execute)

### How to launch overnight reliably (post-S-Auto-9 substrate)

```bash
# Option A: raw bash launcher (preferred for NI=0 hygiene per OQ-zsh-BG_NICE)
cd <repo-root>
bash -c 'nohup python -u -m autoloop run --experiments 15 --auto-reboot > /tmp/sprint-065-overnight.log 2>&1 &'
echo $!  # capture PID

# Option B: S-Auto-9 wrapper pattern (signal-trap + foreground/detached)
cd <repo-root>
scripts/sprint-064-step4-smoke-iter.sh 15
# (Read the wrapper before invoking; understand its signal-trap + cleanup semantics)
```

Monitor in a separate terminal:
```bash
tail -F /tmp/sprint-065-overnight.log
# OR
watch -n 30 'tail -3 /tmp/sprint-065-overnight.log && echo "---" && ps -p <PID> -o pid,etime,rss,command 2>/dev/null'
```

### How to verify substrate health pre-overnight

```bash
# Smoke iter
scripts/sprint-064-step4-smoke-iter.sh 1
# OR
python -m autoloop run --experiments 1 --auto-reboot

# Verify experiments.jsonl row written
tail -1 autoloop/results/experiments.jsonl | python -m json.tool | head -50
# Expected: non-null verdict.layer_results with 5 layers
```

### How to consult propose-distribution

```bash
# Walk autoloop/results/runs/exp-*/ for proposal artefacts
find autoloop/results/runs -name 'proposal*.yaml' -o -name 'proposal*.json' | head -20

# Cf-character scan (R-S58 reopen check)
python -c "
import pathlib, unicodedata
for p in pathlib.Path('autoloop/results/runs').rglob('proposal*'):
    if not p.is_file(): continue
    content = p.read_text(errors='replace')
    cf_chars = [(i, ch, unicodedata.name(ch, '<unnamed>')) for i, ch in enumerate(content) if unicodedata.category(ch) == 'Cf']
    if cf_chars:
        print(f'{p}: {len(cf_chars)} Cf chars; first: {cf_chars[0]}')"
```

### How to read per-turn traces for §5.6 review

The per-turn trace is in `case_results[].per_turn_trace[]` per the eval_interactive result schema. For each kept candidate's eval result file, walk per-case per-turn:

```bash
python -c "
import json, pathlib
result_path = pathlib.Path('eval_interactive/results/<result-dir>/results.json')  # adjust per actual path
data = json.loads(result_path.read_text())
for case in data['case_results']:
    print(f'--- case: {case[\"case_id\"]} ---')
    for turn in case['per_turn_trace']:
        print(f'  turn {turn[\"turn_index\"]}: {turn[\"role\"]} -> {turn.get(\"content_preview\", \"\")[:200]}')"
```

(Adjust schema field names per actual eval_interactive `case_results` shape.)

### How to invoke cherry-pick apply Hybrid mode

```bash
# Per OQ-S55.1: Hybrid mode = cherry-pick + emit baseline patch + NO auto-commit
python -m autoloop apply --experiment exp-<N>

# Verify the apply output:
#   - Skill YAML edit isolated to LLM-soft fields only
#   - Sandbox PASS / anti-hardcode PASS / gaming 0-ERROR
#   - Baseline patch emitted (separately commitable)
#   - No auto-commit (human commits manually)

# Human manual commit
git add server/src/main/resources/skills/<edited-skill>.yaml
git commit -m "Sprint 065 / S-Auto-10 / M-Auto-2 — cherry-pick exp-<N>: <one-line>

<bullet-point body>

Co-Authored-By: <deliver-agent footer>"

# Verify baseline_dir advanced
git log -p autoloop/config.yaml | head -50  # if baseline_dir is tracked there
# OR consult the canonical baseline pointer mechanism per autoloop docs
```

---

## Self-check (verify before claiming done)

- [ ] Pre-flight env check passed: foreground :8080 backend healthy.
- [ ] Smoke iter confirmation: 1 iter reaches Step 9 with non-null 5-layer verdict.
- [ ] Pre-batch baseline rerun ≥2 executed: per-suite loader-counted `case_passed` envelope cited (NOT CLI `Passed:N`).
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
- [ ] scoring SHA reasserted (via `yaml.safe_load(...)` invocation pattern, NOT `from autoloop.config import load`).
- [ ] Java baseline UNCHANGED (zero-touch verified).
- [ ] Hard-fence diff cumulative against all M-Auto-2 §6 17+3 gated paths returns empty (or only the cherry-pick Skill YAML surface).
- [ ] Handoff §0-§12 sections all filled per format.
- [ ] M-Auto-2 §5 acceptance bar S-Auto-10 contribution items ticked off.
- [ ] Local-Mac constraint honored throughout (NO cloud / remote-server framing).
- [ ] cv ceiling discipline honored (NO bump to `length_overflow_absolute_ceiling`).
- [ ] No stale prompt/template references (`yaml.safe_load(...)` + `autoloop/autoloop/scoring/gaming.py`).
- [ ] No fence #20 applier.py re-edits.
- [ ] No hard-fence touches beyond fence #3 cherry-pick Skill YAML mechanism (if cherry-pick landed).

---

## End of dev prompt

Author commits + handoff per §"Handoff requirements" + §"Commit discipline" above. Surface findings + AskUserQuestion-able decisions transparently throughout execution per the fast-iteration STOP-and-surface protocol.
