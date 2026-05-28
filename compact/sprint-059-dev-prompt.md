# Sprint 059 / M-Auto-1B S-Auto-6 — Dev Prompt

## Role identity

你是 **dev agent for Sprint 059 / M-Auto-1B sub-sprint S-Auto-6 — First overnight batch + First human review + First cherry-pick to main**.

**One-sentence goal**: run the first auto-loop overnight batch (≥10 iterations) against the calibrated detector (S-Auto-5 Fix-C step 1 + step 2 Path A `synonym_map_enabled=true`) + accumulate per-iteration verdicts + observation evidence + STOP-and-surface to deliver-agent + human at the §5.6-style manual review of kept candidates + AskUserQuestion cherry-pick decision gates (you do NOT pick the cherry-pick candidate alone; the human selects via AskUserQuestion).

This prompt is your **self-contained executable view** per `iteration_governance.md` §9 invariant. You do NOT need to read any other repo doc (governance chain auto-loaded via `AGENTS.md`). Code anchors are cited inline as `path:line` references for you to read on demand.

S-Auto-6 is a **hybrid dev + deliver-agent + human session**: dev triggers the overnight + observation collection; deliver-agent + human jointly conduct §5.6 manual review + AskUserQuestion cherry-pick decision. The prompt embeds explicit STOP-and-surface points at each human-judgment gate.

## Read order

1. **`AGENTS.md`** (auto-loaded; do NOT re-read explicitly) — Constitution + doc_governance + agent_context_guide + iteration_governance.
2. **THIS prompt** — your full executable contract.
3. **Code anchors on demand** (only read what you need):
   - `autoloop/autoloop/cli.py` — verify `--experiments` flag + any `--baseline-rerun` flag for #1.
   - `autoloop/autoloop/loop.py` — read-only; observe the 14-step state machine logic during overnight (hard-fenced; do NOT modify).
   - `autoloop/config.yaml` — S-Auto-5 close state with `synonym_map_enabled: true`; consult for `fitness.baseline_dir` current value.
   - `autoloop/results/runs/exp-<N>/` — generated artefacts during overnight; consult for per-iter `hypothesis.json` / `diff.yaml` / `iteration_record.json` during manual review.
   - `eval_interactive/results/<run-id>/results.json` — per-suite per-case verdicts during manual review.
   - S-Auto-5 handoff `docs/sprints/sprint-058-handoff.md` §4 + §5 — S-Auto-5 live-iter outcomes + calibration evidence (reference only; informs your understanding of substrate behavior).

You do NOT read: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/teams/deliver-agent.md`, sprint archives, milestone archives, Codex prompt or findings, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/solutions/**`. All required content is embedded below.

## Class

`eval_spec` (§3.2 Q6). **§7 REQUIRED**.

`eval_spec` boundary: sub-sprint consumes per-iteration fitness verdict sequence on a calibrated detector; the cherry-pick mechanism + §5.6 manual review are eval-side acceptance bars, not runtime semantic changes. The cherry-picked Skill YAML edit (if any) is the LLM-soft-narrative surface (§1.3 LLM owns content) — semantic-touching, hence §7 REQUIRED.

## Goal (the contract — verbatim)

By S-Auto-6 close:

1. **Pre-batch baseline drift envelope established.** ≥2 baseline reruns of the v1 47-case fitness suite (`bad_cases` ×12 + `anchor_outcome` ×12 + `shadow` ×23) on the calibrated detector at S-Auto-5 close HEAD `ae0ec3e`; median per-suite `case_passed` count + IQR recorded in handoff §X "Baseline drift envelope". If baseline-vs-baseline drift exceeds 5/34 cases (M-Auto-1A close-day signature), STOP and surface BEFORE starting overnight.

2. **First overnight batch executed.** `python -m autoloop run --experiments <N>` for N in [10, 20] (target ≥10; budget 6-8h). Each iter writes to `autoloop/results/runs/exp-<N>/` + `experiments.jsonl` + `iterations.sqlite` + `lessons.md` (K=10 lessons_compactor auto-triggers if reached). Crash recovery (S-Auto-3 substrate) handles transient errors. Total errors ≤50% acceptable; >50% triggers halt + investigate.

3. **First §5.6-style manual review of kept candidates.** Deliver-agent + human jointly read per-turn traces for EACH kept candidate; judge PASS / FAIL / IMPROVING jointly (NOT programmatic alone); filter through drift envelope from Goal #1; classify as **eligible-for-cherry-pick** / **deferred-to-M-Auto-2+** (kept on `autoloop/keep-<N>` branch) / **discarded**. STOP-and-surface to deliver-agent — dev does NOT judge alone.

4. **First cherry-pick decision via AskUserQuestion.** Deliver-agent surfaces the eligible candidate slate; human selects EXACTLY ONE candidate to cherry-pick OR 0 candidates with explicit justification. If cherry-pick lands: `python -m autoloop apply --experiment exp-<N>` Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit); human manually commits. STOP-and-surface to deliver-agent — dev does NOT pick.

5. **Observation accumulation + R-S58 disposition recommendation.** Per-iteration elapsed time avg; FLAG rate distribution; gaming flag counts + severity; `shadow_disagreement_rate` first measurement; whether NEW bypass variants beyond R-S58 surface from real meta-agent propose distribution. Recommendation on R-S58: (a) defer to M-Auto-2 OR (b) extend M-Auto-1B with S-Auto-7 Fix-D. Final disposition is deliver-agent + human at M-Auto-1B close.

6. **Milestone-shared Codex review at M-Auto-1B close** (deliver-agent dispatches; you do NOT). UNLESS cherry-pick candidate touches §5.3 borderline (per-sub-sprint Codex required BEFORE M-Auto-1B close).

**Zero touch** to all hard-fenced surfaces (see Hard fences section below). **Conditional touch**: EXACTLY ONCE to `server/src/main/resources/skills/*.yaml` via cherry-pick mechanism in Goal #4.

## Scope (numbered execution steps)

### #1 — Pre-batch baseline rerun + drift envelope

First verify CLI flags:

```bash
cd autoloop && uv run --extra dev python -m autoloop --help 2>&1 | head -30
```

Confirm the `run` subcommand supports `--experiments <N>`. If a `--baseline-rerun` flag exists, use it. If NOT, run the eval suites manually:

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1
cd eval_interactive && uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4
cd eval_interactive && uv run eval-interactive run --path case_specs_shadow/ --parallel 4
```

(Note: bad_cases stays `parallel=1` per S-Auto-2 default for `R-bad-case-parallel-session-establishment-flakiness` evidence.)

Run the baseline ≥2 times (cap at 3 if median+IQR width is unstable). Record per-baseline-run aggregate `case_passed` counts per suite by reading each run's `results.json`:

```bash
# After each baseline run, find the run directory:
ls -t eval_interactive/results/ | head -3
# For each: cat eval_interactive/results/<run-id>/results.json | jq '.summary'
```

Compute:
- Median per suite (bad_cases: M_bc; anchor_outcome: M_ao; shadow: M_sh)
- IQR per suite (≥3 runs); for 2 runs, use min/max range as proxy
- Cross-suite total median + IQR

**Acceptance**:
- Baseline-vs-baseline drift ≤ 5/34 cases (the M-Auto-1A close-day signature; 14.7%).

**If drift >5/34**: STOP and surface to deliver-agent + human BEFORE starting overnight; joint decision required.

Record in handoff §X "Pre-batch baseline drift envelope".

### #2 — Pre-overnight smoke + overnight batch

**Pre-overnight smoke MANDATORY**:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 1
```

(NO `--dry-run`.) Measure the full 14-step state machine end-to-end elapsed including the eval cycle. S-Auto-5 only got to Spring spawn before failure (OQ-S58.7); this is the FIRST measurement of a complete iter on `auto-loop-branch` against real LLM. Record:

- Iteration outcome (keep / discard / error).
- Per-iter wall-clock elapsed time.
- `autoloop/results/runs/exp-<smoke>/` artefacts.

**Smoke acceptance**:
- Full iter completes (any verdict — keep / discard / error) without unhandled crash.
- Per-iter elapsed ≤25 min → proceed to overnight at full target count (15).
- Per-iter elapsed 25-40 min → reduce overnight count proportionally (e.g., budget 6h / 30 min/iter = 12 iter).
- Per-iter elapsed >40 min → STOP and surface.

**Overnight batch**:

Based on smoke elapsed, pick `--experiments <N>` value in [10, 20]:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 15
```

(Adjust `15` per smoke evidence.) Each iter appends to `autoloop/results/experiments.jsonl` + `iterations.sqlite` + (if K=10 reached) `lessons.md`.

**Crash recovery + Spring spawn observations during overnight**:
- Transient LLM API errors: S-Auto-3 substrate handles automatically; per-iter state persists.
- Spring spawn failures (OQ-S58.7 pattern): the loop logs the error + proceeds to next iter. Do NOT patch `applier.py` mid-overnight (hard fence).
- Mid-batch crash: state persists in `experiments.jsonl` + `iterations.sqlite`; restart resumes correctly via `run --experiments <N>` (substrate handles idempotency).

**Stop conditions during overnight**:
- Total errors >50% within the first 5 iterations: halt + surface.
- Per-iter average elapsed >40 min sustained: halt + surface.
- Multiple new bypass shapes from real meta-agent propose distribution PASS detector beyond R-S58: record + halt OR continue (deliver-agent + human decide).

Record in handoff §X "Overnight batch record":
- `--experiments <N>` value chosen + rationale (anchored to smoke evidence).
- Per-iteration table: iter_id | target_skill | target_field | iteration_decision | discard_reason | elapsed_s | propose_succeeded | anti_hardcode_flag_for_codex | gaming_flags count.
- Cumulative counts (keep / discard / error / total).
- Whether `autoloop/results/lessons.md` was written (K=10 trigger).

### #3 — STOP and surface: §5.6 manual review of kept candidates

**STOP-and-surface to deliver-agent + human** at this step. Dev session does NOT judge kept candidates alone.

Before surfacing, prepare the review surface:
- List kept candidates: `iter_id` where `terminal_decision == "keep"` per `iterations.sqlite` (or `experiments.jsonl` filter).
- For each, open `autoloop/results/runs/exp-<N>/{hypothesis.json, diff.yaml, sandbox_verdict.json, anti_hardcode_verdict.json, tier_evaluator_verdict.json, iteration_record.json}` for review.
- For each, locate the eval `run-id` referenced in `tier_evaluator_verdict.json` → `eval_interactive/results/<run-id>/results.json` for per-case per-turn detail.
- Optional: open M5 admin trace UI surface for per-invocation LLM raw response (post-M5 S2 deliverable).

Surface summary to deliver-agent + human as a candidate slate table:

```
| iter_id | target_skill | target_field | before→after | Layer 0-4 verdict | gaming_flags | propose-stage flags |
```

Then **WAIT** for deliver-agent + human joint manual review. They will read traces + judge PASS / FAIL / IMPROVING / borderline-§5.3 per candidate.

Record their per-candidate verdict + classification in handoff §X "Manual review of kept candidates":

| iter_id | target_skill | target_field | manual review verdict | classification | rationale |
|---|---|---|---|---|---|
| exp-<N> | ... | ... | PASS / FAIL / IMPROVING / borderline-§5.3 | eligible-for-cherry-pick / deferred-to-M-Auto-2+ / discarded | (deliver-agent + human joint note) |

### #4 — STOP and surface: cherry-pick decision via AskUserQuestion

**STOP-and-surface to deliver-agent** at this step. Dev session does NOT pick the cherry-pick candidate.

Deliver-agent dispatches AskUserQuestion to human with the eligible-for-cherry-pick slate (filtered from #3). Human selects EXACTLY ONE candidate OR 0 candidates with explicit justification.

**WAIT** for AskUserQuestion outcome.

**If cherry-pick lands** (human selects 1 candidate):

```bash
cd autoloop && uv run --extra dev python -m autoloop apply --experiment exp-<N>
```

(Replace `<N>` with the selected candidate's iter id.) Hybrid mode behaviour:
- Cherry-pick `autoloop/exp-<N>` commit content onto `auto-loop-branch` (or whatever the current branch is; verify with `git status` BEFORE apply).
- Emit baseline patch (showing the new Skill YAML state).
- NO auto-commit (per OQ-S55.1 disposition 2026-05-27).

After apply:

```bash
git status        # verify single-file diff on server/src/main/resources/skills/<file>.yaml
git diff          # verify single-field-class change matching exp-<N>'s diff.yaml
```

**Verification**:
- `git diff --stat` shows EXACTLY ONE file modified: `server/src/main/resources/skills/<file>.yaml`.
- `git diff` shows changes ONLY in `procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[*].desc` field paths.

**If verification fails** (multi-file OR field outside white-list): STOP and surface. Applier produced out-of-fence diff (should be structurally impossible per S-Auto-1 sandbox). This is a substrate bug requiring fix-iteration S-Auto-6.1 OR M-Auto-2 substrate hardening.

**If verification PASSES**:

```bash
git add server/src/main/resources/skills/<file>.yaml
git commit -m "$(cat <<'EOF'
Sprint 059 / S-Auto-6 / M-Auto-1B — apply exp-<N> to main

[2-3 sentences describing the cherry-picked edit: target_skill + target_field + edit summary; manual review verdict; deliver-agent + human joint signature]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

(Replace `<N>` with iter id; fill the description.)

Record in handoff §X "Cherry-pick decision":
- AskUserQuestion record (deliver-agent surfaces; human selects).
- Selected candidate's full metadata.
- `python -m autoloop apply` output.
- `git status` + `git diff` verification result.
- Cherry-pick commit SHA + footer.

**If 0 cherry-pick** (human declines all OR no eligible candidates):

No `python -m autoloop apply` is run. Record in handoff §X:
- Per-candidate decline rationale (one line each).
- Deliver-agent + human joint "no human-approved candidate" justification.
- Impact on M-Auto-1B close gate: 0 cherry-pick is PASS if all other gates PASS.

All eligible candidates stay on `autoloop/keep-<N>` branches (if applier creates them) for M-Auto-2+ review.

### #5 — Observation accumulation + R-S58 disposition recommendation

Record in handoff §X "Observation accumulation":

- **Per-iteration elapsed-time average** across overnight (excluding smoke + baseline reruns).
- **Cumulative FLAG rate**: count of `anti_hardcode_flag_for_codex: true` records across overnight + percentage (relative to total iter count).
- **Gaming flag distribution**: count by `severity` (WARN vs ERROR) + by `rule_id` across overnight `iteration_record.gaming_flags`.
- **`shadow_disagreement_rate` first measurement** (§6 architecture-health metric): fraction of kept candidates whose Layer 4 shadow regression was within tolerance but manual review classified as drift / borderline.
- **Real-meta-agent bypass surface check**: scan overnight `experiments.jsonl` for any `hypothesis.proposed_value` that contains zero-width / format-character obfuscation (e.g., `​` / `‌` / `‍` / `‎` / `‏` / `﻿`) OR other potential bypass shapes. Record whether any surfaced.

R-S58 disposition recommendation:

- **Recommend defer-to-M-Auto-2** if: NO new bypass variant from real meta-agent propose distribution AND R-S58 zero-width signature did NOT manifest in any overnight propose.
- **Recommend extend-M-Auto-1B-with-S-Auto-7-Fix-D** if: R-S58 zero-width signature manifested OR new variant surfaces in overnight.

**Final R-S58 disposition is deliver-agent + human at M-Auto-1B close** — NOT a dev-side call. Your recommendation goes into handoff §X for the joint planning decision.

If cherry-pick landed: `autoloop/config.yaml` `fitness.baseline_dir` advance past the new commit. This is a deliver-agent close-bundle action; you do NOT edit `config.yaml` directly unless deliver-agent explicitly delegates.

## Hard fences / STOP conditions

These are non-negotiable. Violating any one triggers immediate STOP + surface.

- **Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (M-Auto-1B fence #13; content-hash locked at `5177b674...`).
- **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py` (S-Auto-1 through S-Auto-5 substrate — UNCHANGED in S-Auto-6).
- **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` / `cli.py`.
- **Zero touch** to `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/{prompts,scripts,config,mock}/**`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-058-*`, milestone archives, `docs/codex-findings.md` scaffold.
- **Conditional touch** to `server/src/main/resources/skills/*.yaml`: EXACTLY ONCE via Goal #4 cherry-pick mechanism. Outside cherry-pick, zero touch.
- **At MOST 1 cherry-pick** (fence #7 / #17 of M-Auto-1B §6).
- **No `git add -A`** — stage explicitly.
- **No new heavy deps** in `autoloop/pyproject.toml`.
- **No LLM call** in any new code (S-Auto-6 is execution-driven; no new code expected beyond handoff documentation).
- **No human-judgment-gate bypass** — dev does NOT pick cherry-pick candidate alone OR judge no-cherry-pick. AskUserQuestion via deliver-agent is the gate per §5.6.
- **No silent skip of pre-overnight smoke** — mandatory before overnight.
- **STOP and surface** conditions:
  - Pre-batch baseline drift envelope width >5/34 cases.
  - Pre-overnight smoke iter elapsed >40 min.
  - Overnight total errors >50% within first 5 iterations.
  - Per-iter average elapsed >40 min sustained.
  - Multiple bypass shapes from real meta-agent propose distribution PASS detector beyond R-S58.
  - Cherry-pick `apply` mechanism produces out-of-fence diff (multi-file OR field outside white-list).
  - Manual review surfaces a borderline §5.3 case requiring deliver-agent + human + Codex per-sub-sprint judgment.

## Test / eval requirements

- **Python autoloop suite UNCHANGED**: `cd autoloop && uv run --extra dev pytest -q` baseline `223 passed, 1 warning` UNCHANGED (S-Auto-6 adds 0 new pytest tests — execution-driven).
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed`.
- **Java baseline UNCHANGED**: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED IF cherry-pick lands (Skill YAML edit is LLM-soft-narrative; Java tests should not regress by construction). Run `cd server && mvn test -B -pl server` AFTER cherry-pick (if any) to verify.
- **Live-iter end-to-end at scale**: ≥10 overnight iterations complete to terminal verdict.
- **§5.6 manual review evidence** at sub-sprint close: per-kept-candidate trace review documented.
- **Shadow regression-safety check at M-Auto-1B close** (separate from S-Auto-6 — milestone-close gate).
- **No live LLM call in pytest tests** — overnight LLM calls happen via `python -m autoloop run` (CLI).

## §7 — Layer-classification + anti-hardcode stanza (REQUIRED)

**Target failure layer:** `eval_spec` (§3.2 Q6). The cherry-picked Skill YAML edit (if any) is LLM-soft-narrative surface (§1.3 LLM owns content); the change is sandbox-validated + anti-hardcode-validated + tier_evaluator-validated + §5.6-manual-reviewed BEFORE landing on main. Cherry-pick is §5.6-gated structured improvement, NOT a §1.7 violation.

**Tier-0 invariant:** adds no Tier-0 invariant. Cherry-picked Skill YAML edit does NOT modify Tier-0 invariants per `docs/runtime_freeze_and_risk_policy.md` §1/§2 (Java-side safety floor). The §3.2 Q2 detector rule REJECTS Tier-0 invention attempts at propose-stage; overnight kept candidates filtered through this.

**Semantic hardcode:** S-Auto-6 introduces NO new semantic hardcode in source code. Cherry-picked Skill YAML edit (if any) passes calibrated anti_hardcode_check (Fix-C step 1 + `synonym_map_enabled=true`) at propose-stage; §5.6 manual review additionally guards against borderline §5.3. Detector + manual review chain is §1.7 enforcement; S-Auto-6 does NOT modify the chain (sandbox + anti_hardcode + content_validator + gaming all hard-fenced).

**Generalization coverage:**

- **target** = (i) ≥10 overnight iterations complete to terminal verdict; (ii) ≥1 §5.6 manual review (or 0 if no kept); (iii) cherry-pick decision (1 or 0) with joint signature.
- **neighbor** = cherry-picked candidate (if any) does NOT regress neighboring cases — related test cases in `bad_cases` / `anchor_outcome` / `shadow` do NOT shift PASS → FAIL beyond drift envelope from Goal #1.
- **negative-control** = cherry-picked candidate does NOT shift the M-Auto-1B close-day bad-case manual review distribution (expected: ≥1 case moves to IMPROVING or PASS IF cherry-pick targets R-iwzx / UC-G/H/I/J-like case; otherwise distribution matches M-Auto-1A close).
- **shadow** = shadow firewall UNCHANGED (no detector / config / scoring change). Overnight kept candidates filter through Layer 4 shadow regression check (≤3% drop); cherry-picked candidate's ≤3% drop guarantee carries to main.

## Codex review plan (§4.3)

**Default**: milestone-shared at M-Auto-1B close. Deliver-agent dispatches; you do NOT.

**Per-sub-sprint Codex CONDITIONAL on cherry-pick borderline-§5.3** — deliver-agent + human jointly judge at AskUserQuestion. If borderline, deliver-agent dispatches per-sub-sprint Codex at S-Auto-6 close BEFORE M-Auto-1B close.

You do NOT decide Codex dispatch.

## Handoff requirements

Author `docs/sprints/sprint-059-handoff.md` at S-Auto-6 close. Leave **§12** empty (deliver-agent + human at milestone close). Required sections:

- **§1 Class + §7 stanza self-walk** — confirm against delivered scope.
- **§2 Goal achievement** — bullet each of the 6 Goal items + evidence pointer.
- **§3 Scope execution log** — for each scope step #1-#5, brief status (DONE / PARTIAL / SKIPPED-WITH-REASON).
- **§4 Pre-batch baseline drift envelope** — per-baseline-run aggregate `case_passed` counts per suite (≥2 reruns); median + IQR per suite + cross-suite total; drift envelope decision.
- **§5 Pre-overnight smoke + overnight batch record** — `--experiments 1` smoke outcome + elapsed; overnight `--experiments <N>` value + rationale; per-iteration table; cumulative counts.
- **§6 Manual review of kept candidates** — per-kept-candidate table with classification.
- **§7 Cherry-pick decision** — AskUserQuestion record; selected candidate (or 0 + justification); apply output; git verification; cherry-pick commit SHA.
- **§8 Observation accumulation** — per-iter elapsed avg; FLAG rate; gaming flag distribution; `shadow_disagreement_rate`; bypass surface check result.
- **§9 R-S58 disposition recommendation** — defer-to-M-Auto-2 OR extend-M-Auto-1B-with-S-Auto-7; rationale.
- **§10 Code anchor table** — `git show --numstat <commits>` for commits during S-Auto-6.
- **§11 Test count** — autoloop pytest UNCHANGED at `223 passed, 1 warning`.
- **§12 OQ-S59.x list** — open questions surfaced; disposition per OQ.

## Commit discipline

Expected pattern: 1 or 2 commits at S-Auto-6 close.

**Commit 1 (CONDITIONAL; cherry-pick only IF candidate selected at #4)**:

```
Sprint 059 / S-Auto-6 / M-Auto-1B — apply exp-<N> to main

[2-3 sentences describing the cherry-picked edit + manual review verdict + joint signature]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage ONLY: `server/src/main/resources/skills/<file>.yaml`.

**Commit 2 (ALWAYS; S-Auto-6 close handoff)**:

```
Sprint 059 / S-Auto-6 / M-Auto-1B — first overnight batch + first human review + first cherry-pick <or "no cherry-pick" + justification>

[2-3 sentences describing overnight outcome + manual review verdict on kept candidates + cherry-pick decision + R-S58 disposition recommendation]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage ONLY: `docs/sprints/sprint-059-handoff.md`.

**No `git add -A`**. Deliver-agent close-bundle artefacts bundled by human at close.

## Self-check checklist (before declaring sub-sprint done)

- [ ] `python -m autoloop --help` verified `--experiments <N>` flag exists; `--baseline-rerun` flag verified or substitute path documented.
- [ ] Pre-batch baseline rerun ≥2 times completed; per-suite `case_passed` counts recorded.
- [ ] Drift envelope median + IQR computed; envelope width ≤5/34 cases (or STOP-surfaced).
- [ ] Pre-overnight smoke `--experiments 1` ran; full 14-step state machine completed (any verdict); elapsed time ≤25 min (or count adjusted / STOP-surfaced).
- [ ] Overnight `--experiments <N>` ran (N ≥10 final completed count).
- [ ] Per-iteration table in handoff §5 with iter_id / target_skill / target_field / iteration_decision / discard_reason / elapsed_s / propose_succeeded / anti_hardcode_flag_for_codex / gaming_flags count.
- [ ] Cumulative counts (keep / discard / error / total) in handoff §5.
- [ ] If K=10 reached: `autoloop/results/lessons.md` LLM-distilled lesson present.
- [ ] STOP-and-surface at Goal #3 manual review gate (deliver-agent + human jointly conducted; per-candidate verdict in handoff §6).
- [ ] STOP-and-surface at Goal #4 AskUserQuestion gate (deliver-agent dispatched; human selected; recorded in handoff §7).
- [ ] If cherry-pick landed: `python -m autoloop apply --experiment exp-<N>` Hybrid output; `git status` + `git diff` verification PASSED (single-file + single-field-class); cherry-pick commit landed via human manual `git commit`.
- [ ] If 0 cherry-pick: explicit deliver-agent + human "no human-approved candidate" justification recorded in handoff §7.
- [ ] Observation accumulation in handoff §8: per-iter elapsed avg / FLAG rate / gaming flag distribution / `shadow_disagreement_rate` / bypass surface check result.
- [ ] R-S58 disposition recommendation in handoff §9 (defer-to-M-Auto-2 OR extend-with-S-Auto-7-Fix-D; rationale).
- [ ] `cd autoloop && uv run --extra dev pytest -q` reports `223 passed, 1 warning` UNCHANGED.
- [ ] `cd eval_interactive && uv run python -m pytest --tb=no -q` reports `486 passed, 3 failed` UNCHANGED.
- [ ] If cherry-pick landed: `cd server && mvn test -B -pl server` reports `1183 / 1 / 0 / 2` UNCHANGED.
- [ ] `git diff --stat -- server/src/main/java/ eval/src/main/java/ autoloop/autoloop/scoring/ autoloop/autoloop/sandbox/ autoloop/autoloop/loop.py autoloop/autoloop/meta_agent/ autoloop/autoloop/memory/ autoloop/autoloop/cli.py eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/` returns empty (hard-fence zero-touch verified).
- [ ] If cherry-pick landed: `git diff --stat` against main shows EXACTLY ONE file modified under `server/src/main/resources/skills/` and the diff is within `procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[*].desc` field paths only.
- [ ] `docs/sprints/sprint-059-handoff.md` §1-§11 filled; §12 empty.
- [ ] No `git add -A` was used; staging was explicit per Commit discipline.
- [ ] Commit messages follow the Sprint 059 / S-Auto-6 / M-Auto-1B template; footer `Co-Authored-By` present on both commits.
- [ ] Surface to deliver-agent + human that S-Auto-6 is closed; await M-Auto-1B close planning.

## OQ (open questions — fill in handoff §12)

Expected OQ-S59.x candidates:

- **OQ-S59.1** — `--baseline-rerun` CLI flag verified OR substitute eval-runner path documented.
- **OQ-S59.2** — Pre-overnight smoke iter elapsed time vs. 12-25 min target.
- **OQ-S59.3** — Overnight `--experiments <N>` value chosen + rationale.
- **OQ-S59.4** — Spring spawn failure rate during overnight (compare to S-Auto-5 OQ-S58.7 single observation 1/3 = 33%).
- **OQ-S59.5** — Whether K=10 lessons_compactor triggered; first lesson content if so.
- **OQ-S59.6** — Kept-candidate count distribution vs. expectations.
- **OQ-S59.7** — Whether any kept candidate touches R-iwzx / R-bad-case-suite-uc-ghij-seed-from-real-sessions natural-target territory.
- **OQ-S59.8** — Whether R-S58 zero-width signature manifested in overnight propose distribution.
- **OQ-S59.9** — Cherry-pick decision rationale + 0-cherry-pick justification (if applicable).
- **OQ-S59.10** — `config.fitness.baseline_dir` advance + new baseline directory path (if cherry-pick lands; deliver-agent close-bundle).

Add OQ entries as ambiguities are encountered; record disposition per OQ.

---

**END OF DEV PROMPT.** This is your full executable contract. Begin with #1 (baseline rerun). STOP-and-surface at the human-judgment gates (Goal #3 + Goal #4). Surface to deliver-agent + human at any STOP condition or ambiguity.
