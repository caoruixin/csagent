---
title: Sprint 065 / S-Auto-10 — transferable context pack (auto-loop fitness debugging)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: this file (session handoff) + docs/sprints/sprint-065-handoff.md
last_reviewed: 2026-05-31
review_cadence: ad hoc
notes: >
  Hand-off context for a FRESH agent continuing Sprint 065 / S-Auto-10 / M-Auto-2.
  Built mid-session after the auto-loop fitness function was found to be (and partly
  fixed from) completely non-functional. Read this top-to-bottom; it is self-contained
  for continuing the work without re-reading the chat. Governance chain (AGENTS.md)
  still applies. The canonical dev contract is docs/sprint_objective.md; the full
  dev-authored archive is docs/sprints/sprint-065-handoff.md (committed).
---

# Sprint 065 / S-Auto-10 / M-Auto-2 — Context Pack

## 背景 / Background

- **Project**: LLM-first customer-service agent for a classifieds marketplace (Java Spring
  backend under `server/`, eval harness under `eval_interactive/`).
- **Auto-evolution loop (`autoloop/`)**: a meta-agent that proposes edits to **Skill YAML
  LLM-soft fields** (`server/src/main/resources/skills/*.yaml`: `$.procedure`,
  `$.grounding_instruction`, `$.escalation_policy`, `$.critical_steps[*].desc`), runs a
  fitness eval against a frozen baseline, and **keeps/discards** each candidate. A human
  later cherry-picks a kept candidate. This is milestone **M-Auto-2** ("Local-Mac OQ-S62.3
  diagnostic + first overnight + first cherry-pick").
- **This sub-sprint = S-Auto-10** (2 of 2-3 in M-Auto-2). Layer = `eval_spec`. Goal was:
  first reliable overnight batch (≥10 iters reaching Step 9) + first §5.6 manual review +
  first cherry-pick decision; retire M-Auto-1C §12.4 deferred gates #2/#3/#4.
- **What actually happened this session**: executing S-Auto-10 surfaced that the auto-loop's
  **candidate fitness eval had NEVER run** (a path bug, present across M-Auto-1A..M-Auto-2),
  so every "verdict" the loop ever produced was degenerate. We fixed that, then found the
  Layer-0 gate was mis-designed (absolute, not delta), fixed that too, then found the
  corrected run was polluted by transient LLM-provider latency. **We have still never
  observed one clean, valid fitness run.** That is the crux for whoever continues.

## The loop pipeline (terminology — needed to read verdicts)

`autoloop run --experiments N --auto-reboot` runs N iterations. Each iteration:
1. propose (meta-agent analyzer + proposer LLM, provider AICodeWith/anthropic-compatible) →
   `hypothesis` {target_skill_file, target_field_path, before_value, after_value, rationale}.
2. `content_validator` (pre-sandbox): length_overflow/underflow, placeholder/deny-list. Reject → discard, NULL verdict.
3. `yaml_diff_validator` sandbox: only the 4 allowed field paths / 6 allowed skill files. Reject → discard.
4. `anti_hardcode_check`.
5. applier: checks out a new `autoloop/exp-N` branch, commits the Skill edit, spawns a **candidate Spring on a free alt-port** (`applier._spawn_spring`, `start_new_session=True`), health-probes it (`_probe_url_is_up`, `httpx.Client(trust_env=False)`).
6. eval: `eval_runner.run_v1_fitness_suite` runs 3 suites via `uv run eval-interactive run --path <suite> --parallel <p>` with `cwd=repo_root/eval_interactive`, env `CSAGENT_BACKEND_URL=http://127.0.0.1:<alt-port>`.
7. **Step 9** = `tier_evaluator.evaluate(current_results, baseline)` → `LexicographicVerdict` with `layer_results` (5 layers). Non-null `verdict.layer_results` == "reached Step 9".
8. `gaming.detect` (observation-only).
9. keep/discard; tag `autoloop/exp-N` branch; checkout back to `auto-loop-branch`.

**Lexicographic verdict (first failing layer short-circuits; keep = all pass):**
- Layer 0 `tier0_safety` — safety floor. `_TIER0_PY_FAMILY` per-case checks: `no_pii_leakage`, `no_human_only_tool_exposure`, `no_critical_policy_violation`, `escalation_compliance`, `phase_transition_validity`. (Java 11-gate replay is skipped — "not produced by eval-interactive".)
- Layer 1 `tier1_outcome` — bad_cases + anchor_outcome programmatic non-regression (delta: `current < baseline` → fail).
- Layer 2 `tier2_critical_flow` — mandatory-failure non-regression.
- Layer 3 `improvement_threshold` — ≥1 case improvement (delta).
- Layer 4 `shadow_regression` — aggregate shadow drop ≤3% (firewall: per-case shadow never in loop-facing verdict).

**Fitness suites** (`config.fitness.suites`, repo-root-relative paths): `bad_cases` (12 cases, parallel 1), `anchor_outcome` (12, parallel 4), `shadow` (`case_specs_shadow/`, 22, parallel 4). **Frozen baseline** `eval_interactive/results/m-auto-1b-baseline-20260529`: bad_cases **5/12**, anchor_outcome **7/12**, shadow **4/22** passing (loader-counted `case_passed`).

## 目标 / Goals

1. **Primary (still open)**: obtain ONE clean, valid post-fix overnight (15 iters) where the
   fitness eval runs correctly AND the LLM env is healthy, to learn whether any candidate
   can become a **keep** on the existing 6-file/4-field surface. 0 keeps so far is NOT yet
   meaningful because every run to date was invalidated by a bug or by infra degradation.
2. Retire M-Auto-2 / M-Auto-1C §12.4 gate **#2 (first overnight ≥10 iter)** with a VALID run
   (current status: ran mechanically but on broken/degraded eval → NOT truly satisfied).
3. Carry the now-functional loop toward the M-Auto-2 close + the deliver-agent close verdict
   (which the human has PAUSED — see Decision Record).

## 已确认事实 / Confirmed Facts (evidence-backed)

### F1. The candidate fitness eval had NEVER run (OQ-S65.5) — ROOT CAUSE of all prior "5→0"
- `config.fitness.suites[].path` is repo-root-relative (`eval_interactive/case_specs/bad_cases/`)
  but `eval_runner.run_suite` invoked eval-interactive with `cwd=repo_root/eval_interactive`
  and passed the path unchanged → it doubled to `eval_interactive/eval_interactive/case_specs/...`
  → `FileNotFoundError`, exit 1, **no results.json written, candidate Spring got 0 requests**.
- The loop then read the **missing** candidate result as `current_passed=0` → spurious
  `tier1_bad_cases_regression_5_to_0` on **every** iteration, regardless of the edit. Across
  M-Auto-1A..M-Auto-2 every Step-9 verdict was this degenerate 5→0. **The loop never once
  evaluated a candidate.** Proven via a `uv` shim capturing exit=1 + the FileNotFoundError
  stderr; candidate Spring logs were 73 lines (startup only, 0 `Creating session`).
- **FIXED (commit b71d6b5)**: `eval_runner.run_suite` now resolves `spec.path` to an absolute
  path (`(_REPO_ROOT / spec.path).resolve()`). Confirmed end-to-end (exp-40: 44 candidate
  session-creates, real verdict). This was the first time the eval ever ran.

### F2. Layer 0 was ABSOLUTE, not a delta (OQ-S65.6) — second structural gate bug
- `tier_evaluator._evaluate_layer0` took only the candidate (no baseline) and failed if ANY
  case in ANY suite (incl. `bad_cases`) had a `_TIER0_PY_FAMILY` check `False` — an absolute
  floor. But the **baseline itself fails** some bad cases (e.g. `cs011_uc_c_faq_miss_not_distress`:
  baseline `escalation_compliance=False` AND `no_pii_leakage=False`). So the floor sat below
  the baseline → no candidate could ever clear it unless it fully FIXED every baseline-failing
  bad-case Tier-0 check. `bad_cases` is the §5.6 human-judgment suite, curated to EXHIBIT
  failures — it must not be an automated absolute safety floor.
- **Human design principle (locked)**: the loop only edits Skill content, so the gate must
  measure **the delta the edit causes** (better/worse vs baseline), and MUST NOT penalize a
  candidate for failures that already existed in the baseline. Pre-existing vs loop-introduced
  must be separated.
- **FIXED (commit 8ff68c0)**: `_evaluate_layer0(current_suites, baseline)` now reads baseline
  per-case Tier-0 results (via `baseline.snapshots[suite].raw_results_json`) and fails ONLY on a
  check that is `False` in the candidate but was `True` in the baseline for that same suite+case
  (newly-introduced violation). Pre-existing baseline failures recorded under
  `python_tier0_family.pre_existing_baseline_failures_ignored` and do NOT discard. Unknown/missing
  baseline status → treated conservatively as a violation (do not mask safety). Verified
  synthetically (cs011-style pre-existing → PASS Layer 0; cs001-style new regression → FAIL).
  Layers 1-4 were already delta-based; only Layer 0 needed this.

### F3. The corrected overnight was polluted by LLM-deadline degradation (OQ-S65.8) — NOT edits
- Corrected overnight (exp-52..58, 7 iters, **0 keeps**) had every candidate die at Layer 0
  `tier0_escalation_compliance`. Reason (verbatim): `escalation_reason cross-family mismatch:
  expected='faq_miss_threshold_exceeded' (bot_limit), actual='service_degraded'`.
- **Root cause = LLM timeouts, proven**: across 230 overnight eval case-runs, **55 hit the
  LLM-timeout give-up** and **120 escalated with `service_degraded`**. The bot's reply was literally
  "Sorry, I'm a bit slow right now. Please try sending that again in a moment." = the
  `LlmDeadlineExceededException` graceful give-up in `ChatController` (`USER_FACING_LLM_DEADLINE_MS = 30_000`),
  and `service_degraded` is the coerced fallback escalation reason (`ToolDispatcher` / `EscalationReasonResolver`).
  A FAQ-skill edit "breaking" UC-D/UC-K shadow cases has no causal path → environmental, not edits.
- **LLM is healthy NOW** (post-stop diagnosis 2026-05-31): a minimal deepseek completion returns
  ~1.0s; `api.deepseek.com` / `api.moonshot.ai` reachable ~0.4s; `DEEPSEEK_API_KEY` set (35 chars).
  The overnight timeouts were transient latency under the loop's sustained multi-hour concurrency
  (bot uses thinking-enabled `deepseek-v4-flash` + 8592-char system prompt + tool-calling, sitting
  near the 30s deadline). This is the "restart backend + healthy LLM = OK" class of infra problem.

### F4. OQ-S65.1 (git index pollution) — found + resolved
- `autoloop run` per-exp `git commit` commits the WHOLE staged index, not just the target Skill
  YAML. Running the loop with a dirty staged index sweeps unrelated staged files into the FIRST
  exp-branch commit, then the checkout cycle reverts them from `auto-loop-branch`. (Unstaged +
  untracked files are SAFE.) This poisoned exp-20 (deliver docs mixed into a Skill-edit commit)
  and reverted the deliver-agent's staged S-Auto-10 setup. Recovered from the exp-20 commit;
  human chose to commit the deliver bundle (9126c6f) to clean the tree.
- **Operational rule**: always run `autoloop run` on a clean tree (commit/stash first). A poisoned
  exp branch must never be cherry-picked. (Saved to agent auto-memory as `autoloop-dirty-index-hazard`.)

### F5. R-S58 disposition: CLOSED-AS-THEORETICAL-ONLY confirmed
- Cf (zero-width / format-control) scan over 33 propose-stage `hypothesis` rows (after_value +
  raw_llm_response + before_value): **0 observations**. Reopen condition not met.

### F6. Test baselines (must be preserved by any change)
- autoloop pytest: **266 passed**. 17-fixture detector sweep: **31 passed**.
  eval_interactive pytest: **486 passed, 3 failed** (the 3 are a documented pre-existing baseline).
  Java: **zero-touch** (1183 / F1 / E0 / S2 preserved by construction).
- `scoring_code_baseline_sha` (over the 4 SHA-locked scoring files) is currently
  **`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`** (was 22548e20… at
  S-Auto-7; → 7b9954f2… after OQ-S65.5; → 35305bd8… after OQ-S65.6). Drift check is silent.

## 决策记录 / Decision Record (human-confirmed)

- **D1 (OQ-S65.1)**: commit the deliver close-bundle now (→ 9126c6f) to clean the tree, rather than
  stash. Deviates from "human bundles deliver files at close" — done at human instruction.
- **D2 (S-Auto-10 cherry-pick)**: **0 cherry-pick** — empty §5.6 slate (0 keeps). (Made BEFORE the
  fitness bug was known; it is moot/invalid because no candidate was ever really evaluated.)
- **D3**: authorize the OQ-S65.5 fix (fence-#13 `eval_runner.py` + SHA rebaseline). DONE (b71d6b5).
- **D4**: authorize the OQ-S65.6 fix (fence-#13 `tier_evaluator.py` Layer-0 delta + SHA rebaseline).
  DONE (8ff68c0). Implement as a DELTA per the human principle (separate pre-existing from
  loop-introduced).
- **D5 (current)**: **STOP** the corrected overnight (polluted by LLM degradation) and diagnose the
  LLM env first; **do NOT change any more gated scoring logic during a run unless a NEW SUBSTRATE
  BUG is proven**; the key durable lesson is the loop must **detect/respond to the exception signal**
  (LLM-deadline / service_degraded / failed-eval) and treat such an iteration as an infra-error,
  never a fitness regression.
- **D6**: the deliver-agent's M-Auto-2 **close verdict is PAUSED** by the human (it had been authored
  as "Class A — Clean PASS, 11/13 gates met" BEFORE these findings → now invalid; must be revised
  after a valid run). Do NOT sync to the deliver agent until a valid overnight completes.

## 当前任务 / Current Task (exact state, 2026-05-31)

- **HEAD = `d4c5e33`** on `auto-loop-branch`. Session commits: `9126c6f` (deliver bundle),
  `4f33e26` (S-Auto-10 handoff), `b71d6b5` (OQ-S65.5 fix), `8ff68c0` (OQ-S65.6 fix),
  `d4c5e33` (handoff OQ-S65.7/S65.8).
- **Working tree**: 3 files modified + UNSTAGED — `docs/10-handoff.md`, `docs/action_bank.md`,
  `docs/milestone_objective.md`. These are **deliver-agent-owned** (M-Auto-2 close-prep). DO NOT
  touch/stage/commit them as dev. They are loop-safe while unstaged (OQ-S65.1).
- `experiments.jsonl` has 58 rows: exp-1..51 = pre-fix degenerate (ignore for fitness);
  exp-52..58 = corrected Layer-0-delta run but LLM-polluted (ignore for fitness).
- No stray processes; `:8080` free; both fixes verified; tests green.
- **PENDING DECISION (where the session paused)**: I recommended re-running the 15-iter overnight
  on a fresh backend + healthy LLM, **with a live external degradation-abort watch** (monitor each
  iter's eval for `service_degraded` / `llm_deadline_exceeded` prevalence; abort early if it
  re-degrades — operationalizes "detect the signal" without touching fenced code). Awaiting the
  human's choice: re-run now, vs restart backend / reduce eval concurrency / raise deadline first.

## 下一步 / Next Steps

1. **Confirm env health, then re-run the 15-iter overnight** (`cd autoloop && nohup caffeinate -i -s
   python -u -m autoloop run --experiments 15 --auto-reboot &`). Pre-flight: nothing staged, :8080
   free, LLM completion < a few seconds. Run from a clean tree.
2. **Live degradation watch** (external, no scoring change): after each iter, scan that iter's
   eval results in `eval_interactive/results/<ts>/` for `service_degraded` / `llm_deadline_exceeded`
   prevalence; if it spikes, abort the run and surface (don't waste ~16 min/iter × remaining).
3. **Per-iteration ledger** (human wants this): for each iter record CHANGED (skill:field, Δchars) +
   INTENT (rationale) + OUTCOME (which layer died + reason) + (Layer 0) the NEW Tier-0 violations vs
   `pre_existing_baseline_failures_ignored`. Regenerate from `experiments.jsonl` rows (hypothesis +
   verdict.tier_breakdown). For Layer-0 deaths, CONFIRM each failing case is baseline-True→candidate-False
   (a true new regression), not pre-existing, and not the `service_degraded` infra artifact.
4. **If any `keep` appears → STOP for §5.6 human review before cherry-pick.** If no keep on a clean
   run → that is the first MEANINGFUL no-keep result (report it; it informs whether the bottleneck is
   proposer quality vs gate design vs surface).
5. After a valid run: update `docs/sprints/sprint-065-handoff.md`, then the human syncs to the
   deliver-agent to revise the (now-invalid) M-Auto-2 close verdict.

## 注意事项 / Notes & Constraints (do not violate)

- **No more gated-scoring-logic changes during a run** unless a NEW substrate bug is proven (human lock).
  The two fixes (eval_runner path, tier_evaluator Layer-0 delta) are done + committed; leave them.
- **Fenced surfaces** (need explicit human authorization to edit): `tier_evaluator.py`, `eval_runner.py`,
  `baseline_loader.py`, `gaming.py` (fence #13, SHA-locked); `applier.py` (fence #20, FINALIZED — DO NOT
  edit, it holds the S-Auto-9 killpg/proxy fixes); `loop.py`, `config.yaml`, `meta_agent/**`, `memory/**`,
  `preflight.py`, `cli.py`; all `server/`, `eval_interactive/eval_interactive/**`, `case_specs/**`,
  `docs/foundational/**`, `docs/current/**`, sprint archives.
- **No `git add -A`** — stage explicitly. **Local-Mac only** (no cloud framing).
- **Do NOT bump `length_overflow_absolute_ceiling` (1200)** on a single anecdote (Codex trigger #3;
  need ≥5-10 propose data points).
- **Use loader-counted `case_passed`** (`autoloop.scoring.baseline_loader.load`), NOT the CLI `Passed:N`
  headline (it is structurally 0 when judge_score is uniform 0; Codex trigger #2).
- **Skill YAML editable ONCE via the cherry-pick mechanism** (`autoloop apply --experiment exp-N`, Hybrid:
  cherry-pick + emit baseline patch + NO auto-commit). Only cherry-pick a clean (Skill-YAML-only) exp branch.
- After editing any of the 4 SHA-locked scoring files, **recompute + update `config.fitness.scoring_code_baseline_sha`**
  (`uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"`).

## Open questions ledger (OQ-S65.x)

- **OQ-S65.1** — loop sweeps dirty staged index into first exp commit. RESOLVED operationally (commit clean tree); durable fix (commit only target path / preflight hard-fail) is carry-forward (fenced).
- **OQ-S65.2** — per-iter eval traces not retained under `runs/exp-N/eval/` (blocks §5.6 trace review). NOTE: post-S65.5 the eval DOES write to `eval_interactive/results/<ts>/`; verify whether the loop now retains a usable pointer. Carry-forward.
- **OQ-S65.3** — "5→0 on any edit": RESOLVED, was the OQ-S65.5 artifact.
- **OQ-S65.4** — meta-agent propose-call `APITimeoutError` ~13% of iters (transient). Consider bounded retry / over-provisioning batch size.
- **OQ-S65.5** — eval never ran (doubled path). **FIXED (b71d6b5).**
- **OQ-S65.6** — Layer 0 absolute not delta. **FIXED (8ff68c0).**
- **OQ-S65.7** — failed/empty suite eval (`exit_code != 0` or missing results.json) is silently scored as `0 passed`; must become iteration `error`. This is what MASKED OQ-S65.5. Carry-forward (fenced `loop.py`/`eval_runner.py`).
- **OQ-S65.8** — corrected overnight polluted by LLM-deadline degradation; loop must DETECT the exception signal (llm_deadline / service_degraded prevalence) and mark the iteration infra-error, not a Tier-0 regression. LLM healthy now; transient under sustained load. Carry-forward.

## Key code anchors (read-only unless authorized)

- `autoloop/autoloop/scoring/eval_runner.py` — `run_suite` (path now absolute; OQ-S65.5 fix).
- `autoloop/autoloop/scoring/tier_evaluator.py` — `_evaluate_layer0(current_suites, baseline)` (delta; OQ-S65.6 fix), `evaluate()` call site.
- `autoloop/autoloop/scoring/baseline_loader.py` — `SuiteSnapshot.raw_results_json`, `BaselineSnapshot`.
- `autoloop/autoloop/loop.py` — orchestrator; L247-260 set `CSAGENT_BACKEND_URL` around the eval; persists row to `experiments.jsonl` (no `eval_results`/`error_tail` persisted — see OQ-S65.7).
- `autoloop/config.yaml` — fitness suites, baseline_dir, SHA, ceiling.
- `eval_interactive/eval_interactive.yaml` — `bot.base_url: ${CSAGENT_BACKEND_URL}` (empty → defaults localhost:8080).
- `eval_interactive/eval_interactive/simulator/agent_client.py` — `httpx.Client(transport=HTTPTransport(proxy=None))` (proxy disabled; not the bug).
- `eval_interactive/eval_interactive/scoring/hard_checks.py` — Tier-0 family check definitions; `escalation_compliance` = escalation_reason must match expected by semantic family (`_ESCALATION_REASON_FAMILY`); cross-family hard-fails.
- `server/.../controller/ChatController.java` — `USER_FACING_LLM_DEADLINE_MS = 30_000`; LlmDeadlineExceededException → "Sorry, I'm a bit slow…" give-up.
- `server/.../service/tools/ToolDispatcher.java` + `EscalationReasonResolver.java` — non-canonical escalation reason → coerced `service_degraded`.
- `docs/sprints/sprint-065-handoff.md` — full dev handoff (§0-§11 + OQ-S65.1..S65.8). Committed.
