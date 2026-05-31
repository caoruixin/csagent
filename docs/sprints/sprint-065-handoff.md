---
title: Sprint 065 / S-Auto-10 / M-Auto-2 — Handoff
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-31
review_cadence: ad hoc
notes: >
  S-Auto-10 sub-sprint 2 of 2-3 in M-Auto-2 (Local-Mac OQ-S62.3 diagnostic +
  first overnight + first cherry-pick). Goal: exercise the now-reliably-
  executable substrate end-to-end via the first overnight batch + first §5.6
  manual review + first cherry-pick decision; retire M-Auto-1C §12.4 deferred
  hard gates #2-#4 (first overnight >=10 iter + first human review + first
  cherry-pick). Layer: eval_spec. §7 stanza REQUIRED (semantic-touching via
  cherry-pick). Codex review plan: milestone-shared at M-Auto-2 close unless a
  borderline-§5.3 cherry-pick fires §4.3 trigger #2.
  KEY OUTCOME: first reliable overnight batch executed (13/15 reached Step 9,
  >=10 gate CLEARED); first §5.6 review + first cherry-pick decision performed
  (slate empty -> 0 cherry-pick, human-justified); R-S58 reconfirmed
  CLOSED-AS-THEORETICAL-ONLY (0 Cf). M-Auto-1C §12.4 deferred gates #2/#3/#4
  RETIRED. Surfaced OQ-S65.1 (loop sweeps dirty staged index; resolved),
  OQ-S65.2/3/4. No code touched; all test baselines preserved.
---

## §0 Sub-sprint summary

- **Sub-sprint**: S-Auto-10 (sub-sprint 2 of 2-3 in M-Auto-2). Layer: `eval_spec`
  (per-iteration fitness verdict consumption on the now-reliably-executable
  substrate; §5.6 manual review + cherry-pick are the eval-side acceptance bars;
  semantic-touching via cherry-pick on Skill YAML LLM-soft fields if it lands).
- **Goal**: first overnight batch (>=10 iter reaching Step 9) + first §5.6 manual
  review of kept candidates + first cherry-pick decision via AskUserQuestion.
  Retire M-Auto-1C §12.4 deferred hard gates #2 (first overnight >=10 iter), #3
  (first human review), #4 (first cherry-pick).
- **Outcome**: **DONE**. First reliable overnight batch (13/15 Step-9; >=10 gate
  cleared), first §5.6 review + first cherry-pick decision (empty slate -> 0
  cherry-pick), R-S58 reconfirmed CLOSED-AS-THEORETICAL-ONLY. Gates #2/#3/#4
  retired. Substrate held (no killpg/Spring-timeout). 0 code touched.
- **Cumulative commits**:
  - `9126c6f` deliver close-bundle (S-Auto-9 close + S-Auto-10 setup), committed
    by this dev session at human instruction to clean the tree (see §9 / OQ-S65.1).
  - `<handoff commit>` this handoff (docs/sprints/sprint-065-handoff.md).
  - NO cherry-pick commit (0 cherry-pick); NO code commit (Java + eval zero-touch).
- **Final test counts**: autoloop pytest **266 passed**; 17-fixture detector sweep
  **31 passed**; eval_interactive **486 passed, 3 failed** (UNCHANGED baseline);
  scoring SHA reasserted `22548e20…`; Java zero-touch (not re-run; baseline
  `1183/F1/E0/S2` preserved by construction).
- **Overnight iter counts**: first batch 15 launched / 13 reached Step 9 / 2
  meta-LLM APITimeout errors / 0 keeps. Optional second batch: SKIPPED (human
  decision; §5).
- **§5.6 manual review summary**: kept-candidate slate EMPTY (0 keeps across all
  37 iters; all Step-9 iters short-circuit at Layer 1 tier1 5->0). Review of the
  discard pattern + verdict structure performed; per-turn trace review not
  possible (OQ-S65.2).
- **Cherry-pick decision**: **0 cherry-pick** (empty slate; human-justified via
  AskUserQuestion 2026-05-31). `config.fitness.baseline_dir` unchanged.
- **R-S58 disposition**: CLOSED-AS-THEORETICAL-ONLY confirmed (0 Cf observations).
- **M-Auto-1C §12.4 deferred hard gates #2-#4 retirement**: #2 (first overnight
  >=10 iter) ✅; #3 (first human review) ✅ (performed; empty slate); #4 (first
  cherry-pick) ✅ (decision = 0 with justification). All three RETIRED.

## §1 Pre-flight + pre-batch baseline drift envelope

### §1.1 Pre-flight env check

- `python -m autoloop preflight` (2026-05-31 ~14:00): **5 OK / 1 WARN / 0 FAIL**.
  - OK: `foreground_backend` (no :8080 listener — alt-port spawn unobstructed),
    `postgres_reachable` (localhost:5432), `redis_reachable` (localhost:6379),
    `meta_llm_api_key` (AUTOLOOP_META_LLM_API_KEY set, len=32),
    `baseline_dir_loads` (run_id=m-auto-1b-baseline-20260529, 3 suites).
  - WARN: `clean_working_tree` (7 uncommitted deliver-agent files at session
    start). This WARN became load-bearing — see §9 / OQ-S65.1. After committing
    the deliver bundle (9126c6f) the tree is clean and this check now passes.

### §1.2 Smoke iter Step-9 confirmation

Substrate confirmed healthy + reaching Step 9 post-S-Auto-9 (fence #20 fixes
verified present in `applier.py`: `start_new_session=True` L433, per-port
`spring-boot-{port}.log` L416, `httpx.Client(trust_env=False)` L484):

| iter   | decision | discard_reason                        | reached Step 9 (verdict.layer_results) | elapsed |
|--------|----------|---------------------------------------|----------------------------------------|---------|
| exp-19 | discard  | content_validator.length_overflow     | NO (NULL verdict; pre-Step-9 reject)   | 66.2s   |
| exp-20 | discard  | tier1_bad_cases_regression_5_to_0     | YES (5-layer)                          | 87.3s   |
| exp-21 | discard  | tier1_bad_cases_regression_5_to_0     | YES (5-layer)                          | 87.2s   |
| exp-22 | discard  | tier1_bad_cases_regression_5_to_0     | YES (5-layer)                          | 198.5s  |

- Post-fix Step-9-reach rate is high (exp-18 at S-Auto-9 close + exp-20/21/22 =
  4 of 5 reached Step 9; exp-19 rejected at content_validator length_overflow).
- 0 substrate errors (no killpg suicide, no Spring timeout, no infra error).
- **cv data point #1 (Codex trigger #3)**: exp-19 rejected at
  `content_validator.length_overflow` (ceiling 1200). Recorded; **NO ceiling
  bump** (need >=5-10 data points per Codex M-Auto-1C trigger #3). Consistent
  with the exp-13 (S-Auto-8) precedent and the lessons L-2026-05-31-001
  observation that expansive `$.procedure` rewrites bloat past length limits.

### §1.3 Frozen baseline reference (drift envelope anchor)

Loader-counted `case_passed` (via `autoloop.scoring.baseline_loader.load`, NOT
the CLI `Passed:N` headline — per Codex M-Auto-1C trigger #2), baseline
`m-auto-1b-baseline-20260529`:

| suite          | baseline case_passed | total |
|----------------|----------------------|-------|
| bad_cases      | 5                    | 12    |
| anchor_outcome | 7                    | 12    |
| shadow         | 4                    | 22    |
| **total**      | **16**               | **46**|

### §1.4 Pre-batch reruns (>=2 per suite) + drift envelope

Backend :8080 brought up via `mvn -pl server spring-boot:run` (clean boot 2.5s,
PostgreSQL 17.5, Flyway clean). 2 reruns per suite at baseline parallelism
(bad_cases parallel 1; anchor_outcome / shadow parallel 4). Loader-counted
`case_passed` (= `sum(c.case_passed is True)` per `baseline_loader.py:213`; NOT
CLI `Passed:N`):

| suite          | frozen baseline | rerun 1 | rerun 2 | result dir(s)                          | max Δ |
|----------------|-----------------|---------|---------|----------------------------------------|-------|
| bad_cases      | 5/12            | 4/12    | 5/12    | 20260531-061914 / 20260531-062507      | 1     |
| anchor_outcome | 7/12            | 5/12    | 5/12    | 20260531-063122 / 20260531-063237      | 2     |
| shadow         | 4/22            | 2/22    | 2/22    | 20260531-063446 / 20260531-063832      | 2     |

- **Drift envelope: ~5 cases / 46 (~11%)** absolute deviation from the frozen
  baseline — **well under the 10/34 (~30%) halt threshold. NO HALT.**
- anchor_outcome (7->5) and shadow (4->2) drifted DOWN ~2 cases each,
  **consistently across both reruns** (not run-to-run noise). This matches the
  documented M-Auto-1A external-LLM-provider + judge-calibration variance
  (moonshot-v1-32k; baseline blessed 2026-05-29, 2 days prior) — the §5.5
  rationale's known confound, not a sprint-side cause.
- bad_cases (the tier1 gate surface where all overnight discards fire) is STABLE
  at 4-5 vs frozen 5, so the overnight `tier1_bad_cases_regression_5_to_0`
  verdicts compare candidates against an interpretable, un-drifted baseline.
- **Caveat for verdict interpretation**: the overnight compares candidates vs
  the FROZEN baseline (bad=5/anchor=7/shadow=4), which is ~2 cases optimistic on
  anchor/shadow relative to today's environment; immaterial to the tier1
  short-circuit (bad_cases stable).

## §2 First overnight batch

- **Launch**: `caffeinate -i -s python -u -m autoloop run --experiments 15
  --auto-reboot` (2026-05-31 14:44, PID 92630), on a clean tree (post-9126c6f).
  Preflight 5 OK / 1 WARN (the 1 = my untracked handoff file; loop-safe) / 0 FAIL.
- **Total wall**: 2628s (~44 min) for 15 iter (~175s/iter avg, incl. full eval
  phase per Step-9 iter). NOTE: per-iter `elapsed_seconds` is 0.0 in the JSONL
  rows (not persisted per-row; the CLI `[run]` summary carries it). Smoke
  per-iter times were 66-198s; the eval phase dominates for Step-9 iters.
- **Result**: `[run] done: keep=0 discard=13 error=2 (of 15 total)`.

| iter   | decision | reached Step 9 | reason / error                       |
|--------|----------|----------------|--------------------------------------|
| exp-23 | discard  | YES (5-layer)  | tier1_bad_cases_regression_5_to_0    |
| exp-24 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-25 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-26 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-27 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-28 | error    | NO             | APITimeoutError (meta-agent LLM)     |
| exp-29 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-30 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-31 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-32 | error    | NO             | APITimeoutError (meta-agent LLM)     |
| exp-33 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-34 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-35 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-36 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |
| exp-37 | discard  | YES            | tier1_bad_cases_regression_5_to_0    |

- **>=10 Step-9 gate: CLEARED — 13 of 15 reached Step 9** with non-null 5-layer
  `verdict.layer_results`. **M-Auto-1C §12.4 deferred hard gate #2 RETIRED.**
- **2 errors** (exp-28, exp-32): both `APITimeoutError` on the meta-agent LLM
  propose call (Anthropic-compatible provider network/timeout) — transient
  external-API noise, NOT substrate brittleness (no killpg suicide, no Spring
  timeout). Below the ">50% errors in first 5 iters" STOP threshold (first 5 all
  reached Step 9). Not a STOP condition.
- **0 keeps**: all 13 Step-9 iters short-circuited at Layer 1
  (`tier1_bad_cases_regression_5_to_0`); none reached Layer 2/3/4. The tier
  evaluator correctly gated harmful edits. Empty §5.6 kept-candidate slate.
- **Lessons compaction working (K=10 fired twice across the overnight)**:
  - `L-2026-05-31-001` (exp-11..20): expansive `$.procedure` rewrites bloat past
    length limits or strip bad-case behaviors; `escalate.yaml` prone to overflow.
  - `L-2026-05-31-002` (exp-21..30): edits to ANY prose field
    (`critical_steps[*].desc` / `escalation_policy` / `grounding_instruction` /
    `procedure`) across intake/triage/FAQ skills consistently collapse all
    bad-case fixtures 5->0; heuristic now steers toward additive, narrowly-scoped
    clauses preserving verbatim wording. (K=20-equivalent exercise via two K=10
    windows.)

## §3 Optional second batch (if run)

**SKIPPED** (human decision via the §5 AskUserQuestion, 2026-05-31). Rationale:
0 keeps across 35 prior iters + the L-002 systematic-regression pattern make a
second batch very likely to repeat the null result; marginal value low. The
first batch already cleared the >=10 Step-9 gate and produced a clean (if null)
§5.6 slate, the R-S58 Cf-scan distribution (33 rows), and the K=10/K=20 lessons
exercise. No second batch run.

## §4 Propose-distribution scan (R-S58 reopen check)

- **Scan target (corrects the prompt's suggested `proposal*.yaml` glob)**: there
  are no `proposal*.yaml` files. The propose-stage artefact is each iteration's
  `hypothesis` (`after_value` + `raw_llm_response` + `before_value`), carried
  both in the `experiments.jsonl` rows and (when persisted) `runs/exp-*/hypothesis.json`.
- **Result**: scanned **33 experiments.jsonl hypothesis rows** + 7 runs/ dir
  hypothesis.json. **Cf (zero-width / format-control, `unicodedata.category=='Cf'`)
  observations: 0.** Consistent with the M-Auto-1C S-Auto-8 baseline scan (0/13).
- **R-S58 reopen condition NOT met** -> stays CLOSED-AS-THEORETICAL-ONLY (see §7).

## §5 §5.6 manual review + cherry-pick decision

- **Kept-candidate slate: EMPTY.** 0 keeps across all 37 iterations (exp-1..37).
  Every Step-9 iter (exp-18, exp-20..27, exp-29..31, exp-33..37 = 13 overnight +
  3 smoke) short-circuited at Layer 1 (`tier1_bad_cases_regression_5_to_0`);
  none reached Layer 2/3/4, so none is a `keep`.
- **§5.6 manual review performed (outcome: empty slate)**: with 0 keeps there
  are no per-turn traces of a kept candidate to read. The review instead
  examined: (a) the discard verdict structure — Layer 0 PASS (safety floor
  clean), Layer 1 FAIL (bad_cases 5->0), confirming the tier evaluator gates
  harmful edits correctly; (b) the lessons L-001/L-002 generalization (any prose
  edit on intake/triage/FAQ skills collapses all bad-case fixtures); (c) the
  0-keep outcome is stable across 37 iters and 4 distinct field types. Per-turn
  trace review of an actual candidate was NOT possible (OQ-S65.2: eval/ dirs
  empty) and would block the FIRST keep-candidate review whenever one lands.
- **Per-candidate PASS/FAIL/IMPROVING/borderline classification**: N/A (empty slate).
- **Cherry-pick decision — AskUserQuestion (2026-05-31)**: human selected
  **"0 cherry-pick, skip 2nd batch, close"**. Justification: empty §5.6 slate;
  no candidate even passed Layer 1, so none meets the §5.6 PASS bar; producing a
  keep requires meta-agent proposer tuning (out of S-Auto-10 scope). Optional
  second batch skipped (would very likely repeat the null result given 37 prior
  iters + L-002). OQ-S65.3 (5->0 attribution) investigation declined for now;
  logged for future. **M-Auto-1C §12.4 deferred gates #3 (first human review)
  and #4 (first cherry-pick) RETIRED** — both resolve as "first review/decision
  performed; outcome = empty slate / 0 pick with justification".

## §6 Cherry-pick apply (if any)

- **N/A — no cherry-pick landed.** No `autoloop apply` invoked; no human commit;
  `config.fitness.baseline_dir` UNCHANGED (`m-auto-1b-baseline-20260529`); no
  Skill YAML edited on auto-loop-branch (server/ zero-touch verified, §10).
- CAUTION carried forward (OQ-S65.1): had a keep landed, exp-20's branch is
  poisoned with deliver docs and must never be cherry-picked; only clean
  Skill-YAML-only exp branches (e.g., exp-21+) are cherry-pickable, and only on
  a clean tree.

## §7 R-S58 disposition

- **CLOSED-AS-THEORETICAL-ONLY confirmed.** The §4 propose-distribution Cf-scan
  found **0 zero-width / format-control characters** across 33 experiments.jsonl
  hypothesis rows (after_value + raw_llm_response + before_value) + 7 runs/ dirs,
  covering the full M-Auto-2 propose distribution to date (exp-1..37). The
  reopen condition (>=1 Cf observation in a real propose artefact) is NOT met.
- No re-promotion to ACTIVE; no Fix-D follow-on required; not in scope for any
  S-Auto-11 buffer.

## §8 OQs surfaced

### OQ-S65.1 — `autoloop run` sweeps a dirty staged index into the first exp-branch commit

- **Observed**: running `autoloop run` while the working tree had the
  deliver-agent's staged close-bundle caused the loop's per-exp `git commit`
  (which commits the WHOLE index, not just the target Skill YAML) to capture the
  3 staged deliver files (`docs/sprint_objective.md` +446, `docs/sprints/sprint-064-objective.md`
  add, `compact/sprint-065-objective.md` deletion) onto the FIRST exp branch
  (exp-20, commit `483c321`). The subsequent checkout cycle back to
  auto-loop-branch then reverted those staged changes from the working tree
  (they live only in `483c321` + reflog). Unstaged + untracked deliver files
  survived (the loop's `git commit` only captures the index).
- **Root cause**: the loop's branch-commit step does `git commit` over the full
  staged index rather than `git commit -- <target_skill_path>`. Preflight WARNs
  on a dirty tree but does not enforce a clean/stashed index.
- **Impact / cherry-pick implication**: exp-20's branch is poisoned (contains
  deliver docs intermixed with the Skill edit) and MUST NEVER be cherry-picked.
  exp-21/22 branches are clean (Skill YAML only) because the index was empty of
  deliver changes after exp-20 consumed them.
- **Resolution this sprint**: deliver-agent staged work recovered verbatim from
  `483c321` (git status restored to exact session-start state), then committed
  as `9126c6f` at human instruction (see §9) so the loop runs on a clean tree.
- **Narrow fix recommendation (future sub-sprint; do NOT execute here — fence
  #18/#20 applier surface FINALIZED)**: loop should commit only the target Skill
  path (`git commit -- <path>`) OR stash non-target changes internally; preflight
  `clean_working_tree` should hard-FAIL (or auto-stash) on a dirty index before a
  live `run`, not merely WARN. Routed to `action_bank.md` as an R-item candidate.

### OQ-S65.2 — Per-iter eval traces not persisted (observability debt; blocks §5.6 trace review)

- **Observed**: `autoloop/results/runs/exp-*/eval/` directories are created but
  **empty** after the verdict is computed (exp-23/30/37 each: 0 files). The
  per-suite `results.json` (with `case_results[].per_turn_trace[]`) is consumed
  during scoring and not retained. The `audit --include-shadow-detail` path
  expects `runs/exp-N/eval/shadow/results.json`, which is therefore absent for
  these iters.
- **Impact**: the §5.6 manual-review process (read `per_turn_trace[]` per kept
  candidate) is **not executable post-hoc** from persisted artefacts. Moot this
  sprint (0 keeps), but it is a hard blocker the first time a keep candidate
  lands and a human needs to read its turns before cherry-pick.
- **Recommendation (future sub-sprint)**: persist per-suite `results.json` for
  every `keep` candidate (and ideally a sample of discards) under
  `runs/exp-N/eval/`. Matches the broader observability-debt pattern (eval/admin
  surfaces lag the architecture).

### OQ-S65.3 — "Any prose edit collapses all bad_cases 5->0" — real regression vs eval artifact unverified

- **Observed**: every Step-9 iter across exp-18/20..37 discarded at
  `tier1_bad_cases_regression_5_to_0`; lessons L-002 generalizes that edits to
  ANY prose field (4 field types, 3 skills) collapse all bad_cases to 0. The
  candidate Spring starts and serves a clean verdict (not an error), so this is
  NOT a Spring-startup failure.
- **Open question**: cannot distinguish (a) genuine semantic regression (brittle,
  marginally-passing curated hard fixtures tipping over on any wording shift —
  plausible given baseline is only 5/12) from (b) a candidate-eval artifact
  (e.g., edited Skill subtly mis-served per-request) — because the eval traces
  are not persisted (OQ-S65.2). S-Auto-9 accepted the same 5->0 pattern as a
  valid Step-9 verdict at close, so this sprint treats it consistently as a
  legitimate (if blunt) tier1 verdict, but flags the attribution as open.
- **Recommendation**: resolve jointly with OQ-S65.2 (persist traces), then attribute
  one 5->0 candidate's per-turn failures (semantic miss vs malformed bot reply).

### OQ-S65.4 — Meta-agent LLM propose-call APITimeoutError (~13% of overnight iters)

- **Observed**: 2 of 15 overnight iters (exp-28, exp-32) errored with
  `APITimeoutError` on the meta-agent propose LLM call (request_timeout_seconds
  120 in config). Transient; does not halt the loop (next iter proceeds).
- **Impact**: reduces Step-9 yield per batch (~13% attrition here). Not a
  substrate fault. If a future batch needs a guaranteed iter count, consider a
  bounded retry on the propose call OR over-provisioning the batch size.

## §9 STOP-and-surface log

- **2026-05-31 ~14:15 — OQ-S65.1 (git index pollution)**: surfaced via
  AskUserQuestion after detecting the deliver-agent's staged close-bundle had
  been swept into exp-20 + reverted. Recovered the work from `483c321` BEFORE
  asking (no data at risk). Presented 3 options (stash around runs / pause for
  human / commit deliver bundle now). **Human chose "Commit deliver bundle now"**
  -> committed `9126c6f`; tree clean for the overnight. exp-20 flagged
  non-cherry-pickable.

- No further STOP-and-surface events. The 2 overnight `APITimeoutError`s
  (exp-28/32) were transient meta-LLM provider timeouts, not substrate
  brittleness (OQ-S65.4), below the ">50% errors in first 5 iters" threshold —
  not a STOP condition.
- The cherry-pick / second-batch / 5->0-investigation decision was surfaced via
  the §5 AskUserQuestion (2026-05-31); human chose "0 cherry-pick, skip 2nd
  batch, close".

## §10 M-Auto-2 close-readiness checklist (S-Auto-10 contribution)

Against the M-Auto-2 `milestone_objective.md` §5 13 hard gates:

1. **Tier-0 safety floor unchanged** (zero java/eval-code edits): ✅ S-Auto-10
   landed no cherry-pick; `git diff --stat 38578bc..HEAD -- server/ eval/src/main/java/`
   returns EMPTY. (Verified §10 evidence.)
2. **Java test baseline preserved** (`1183 / F1 / E0 / S2`): ✅ by construction
   (Java zero-touch). Not re-run (no java edit); baseline preserved.
3. **Python test baseline preserved** (eval_interactive `486 passed, 3 failed`;
   autoloop +0 at S-Auto-10): ✅ eval_interactive **486 passed, 3 failed**
   (UNCHANGED — the 3 are documented pre-existing baseline failures:
   test_smoke_review_report_tracks_smoke_set_and_overrides,
   test_full_corpus_lints_clean_with_smoke_subset_flag, +1); autoloop **266 passed**
   (+0 new tests); 17-fixture sweep **31 passed**; scoring SHA reasserted.
4. **OQ-S62.3 RESOLVED** (S-Auto-9 close gate): ✅ retired at S-Auto-9 (N/A to S-Auto-10).
5. **Live iteration end-to-end through Step 9** (gate #1): ✅ retired at S-Auto-9;
   re-demonstrated by 13 Step-9 reaches this sub-sprint.
6. **Pre-batch baseline drift envelope ≥2 reruns**: ✅ S-Auto-10 — ~5/46 drift
   (<10/34 halt threshold); per-suite envelope in §1.4.
7. **First overnight batch ≥10 iter**: ✅ S-Auto-10 — **13/15 reached Step 9**
   (gate #2 RETIRED).
8. **First human review of kept candidates** (gate #3): ✅ S-Auto-10 — review
   performed; slate empty (0 keeps); see §5 (gate #3 RETIRED).
9. **First cherry-pick decision via AskUserQuestion** (gate #4): ✅ S-Auto-10 —
   human selected 0 with explicit justification; see §5 (gate #4 RETIRED).
10. **Curated bad-case suite manual review pass (PRIMARY GATE per §5.6)**: ⏳ at
    M-Auto-2 CLOSE (deliver+human). S-Auto-10 contributes the bad_cases rerun
    data (4-5/12, §1.4); the formal manual-review pass is a milestone-close artefact.
11. **Shadow regression-safety gate** (shadow drop ≤3% if cherry-pick landed):
    ✅ S-Auto-10 — no cherry-pick landed → no NEW shadow regression introduced.
    The shadow rerun 4->2 vs frozen baseline is environment/provider variance
    (§1.4), not a candidate-induced regression.
12. **R-S58 final disposition reconfirmed**: ✅ S-Auto-10 — 0 Cf observations →
    CLOSED-AS-THEORETICAL-ONLY confirmed (§7).
13. **Milestone-shared Codex review at M-Auto-2 close**: ⏳ deferred to M-Auto-2
    close per §4.3 default (no per-sub-sprint trigger fired: no Tier-0 candidate,
    no §1.7 red line, no hard-fence touch, S-Auto-9 Codex was not fix_required).
    The dev does NOT dispatch Codex; deliver+human dispatch at close over the
    cumulative S-Auto-9 + S-Auto-10 range.

**S-Auto-10 verdict**: all S-Auto-10-owned gates clear. The two remaining open
gates (#10 bad-case PRIMARY manual review; #13 Codex) are M-Auto-2-close
artefacts owned by deliver+human, not by this dev sub-sprint.

## §11 Stage-2 entry decision direction

Evidence base: M-Auto-1B (substrate built) + M-Auto-1C (Class-C in-flight
downgrade) + M-Auto-2 (OQ-S62.3 fix + first reliable overnight). The substrate
is now **reliably executable** (13/15 Step-9; no killpg suicide; ~175s/iter).
**The binding constraint is no longer execution — it is propose quality.**

- **Do NOT widen the mutable surface yet** (Stage-2 = unlocking additional Skill
  files / fields per `config.yaml` `mutable_surface` + `program.md` §8). Across
  37 iters the proposer has produced **0 keeps**: every prose edit to
  intake/triage/FAQ skills collapses all bad_cases (lessons L-001/L-002).
  Widening the surface now would only multiply 0-keep iters over more files.
- **M-Auto-3 should target the PROPOSER, not the surface**: leverage the
  lessons memory (already learning "additive, narrowly-scoped, verbatim-preserving
  diffs") more directly — e.g., a minimal-diff / additive-only propose constraint,
  or a pre-propose bad-case-clause-preservation check. Goal: produce the FIRST
  keep candidate on the EXISTING 6-file / 4-field surface before unlocking more.
- **Prerequisite observability (resolve before/with M-Auto-3)**: OQ-S65.2 (persist
  per-iter eval traces) is a hard blocker for the §5.6 review the first time a
  keep lands; OQ-S65.3 (attribute the 5->0 collapse: genuine regression vs eval
  artifact) determines whether the tier1 gate is giving meaningful fitness signal
  at all — both should be closed before declaring Stage-2 readiness.
- **Substrate-hygiene fix (OQ-S65.1)**: the loop's dirty-index sweep should be
  fixed (commit only the target Skill path; preflight hard-fail/auto-stash on
  dirty index) so future overnights are safe to launch without a manual clean-tree
  step.

## End of handoff
