# Codex review prompt — Sprint 060 / M-Auto-1B S-Auto-7 — Per-sub-sprint Anti-Hardcode review (§4.3 trigger #3)

You are the **Anti-Hardcode Per-Sub-Sprint Review Agent for Sprint 060 / M-Auto-1B S-Auto-7**. This is a **per-sub-sprint review** dispatched per `docs/current/iteration_governance.md` §4.3 trigger #3 — the S-Auto-7 sub-sprint touches **two hard-fenced surfaces that M-Auto-1B's milestone objective explicitly named out of scope**, and **MUST be Codex-verified BEFORE the S-Auto-8 overnight batch (the deferred original S-Auto-6 scope) can dispatch**:

- **Fence #13 controlled override** on `autoloop/autoloop/scoring/eval_runner.py` (pre-blessed at S-Auto-7 planning round 2026-05-29 via in-place milestone_objective.md revision; documented Blocker B fix path (b) human-locked at S-Auto-6 close).
- **Fence #2 in-session override** on `eval_interactive/eval_interactive/case_spec/loader.py` (Blocker C; in-session human-authorized during the §3 baseline blessing step — "尽快帮我修复掉吧 / fix it now"; NOT pre-blessed at planning round; substrate path-handling repair).

S-Auto-7 is classified `infra` and `§7 EXEMPT` per the pure-infra carve-out — so the §4.1 nine-question kernel's scope-exemption clause technically applies as the default-return path. **§4.3 trigger #3 overrides that exemption**: Codex must still verify that the two fence overrides are genuinely substrate plumbing (NOT smuggling a semantic surface under an "infra" framing), that the rebaselined `scoring_code_baseline_sha` is reproducible, and that the blessed `baseline_dir` is well-formed for downstream consumption. The milestone-shared Codex review (cumulative S-Auto-5 + S-Auto-6 + S-Auto-7 + S-Auto-8 range) is a **separate** later pass dispatched at M-Auto-1B close; this prompt covers S-Auto-7 ONLY.

The cumulative commit range under review for THIS Codex pass:

```
b0a3704  Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker A baseline_dir blessing + Blocker C loader fix + handoff
559927a  Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker B fix path (b) + scoring SHA rebaseline + tests
```

(parent: `60c5b67` — out-of-scope architecture README commit; itself not part of S-Auto-7; see Axis I below for handling.)

Cumulative S-Auto-7 diff (the 2 commits above only; excludes `60c5b67` README and `b6084f9` S-Auto-6 deliver close-bundle):

```
 autoloop/autoloop/scoring/eval_runner.py           | 104 +++-  (+88 / −16)
 autoloop/config.yaml                               |  35 +-   (+25 / −10 across two commits)
 autoloop/tests/test_eval_runner.py                 | 190 ++++  (+185 / −5)
 docs/sprints/sprint-060-handoff.md                 | 594 ++++  (NEW)
 eval_interactive/eval_interactive/case_spec/loader.py | 18 +-  (+14 / −4)
```

5 files changed; ~+312 / −35 net effective code-side LOC (excluding handoff doc).

---

## Read order (minimal)

Read only:

1. **`AGENTS.md`** (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. **This prompt** (self-contained executable view per `iteration_governance.md` §9 invariant).
3. **`docs/sprints/sprint-060-handoff.md`** — dev's own S-Auto-7 close handoff (single source you cannot embed because it post-dates this prompt's authoring; read in full, especially §1 self-walk, §4 Blocker B evidence, §5 SHA rebaseline evidence, §6 baseline blessing evidence, §7 Goal #4 BLOCKED record, §8 adversarial spot-check, §12 OQ-S60.x list).

You may sample (NOT embed) the following code paths for verification:

- **`autoloop/autoloop/scoring/eval_runner.py`** — the file with Blocker B fix path (b). Specifically: subprocess `cmd` list no longer contains `--output-dir`; new helper `_locate_new_results_dir(ei_results_dir, before_ts_dirs)` using set-diff + mtime tiebreaker; symlink staging at `suite_link.symlink_to(new_ts_dir.resolve(), target_is_directory=True)` with defensive `shutil.rmtree` for pre-existing real dirs; `SuiteRunResult.results_dir` / `results_json` preserve historical `<results_root>/<suite>/results.json` contract.
- **`autoloop/autoloop/scoring/{tier_evaluator,baseline_loader,gaming}.py`** — the OTHER three scoring files in the fence #13 content-hash group. Verify byte-identical to S-Auto-6 close (`git diff --stat b6084f9..HEAD -- autoloop/autoloop/scoring/tier_evaluator.py autoloop/autoloop/scoring/baseline_loader.py autoloop/autoloop/scoring/gaming.py` returns empty).
- **`autoloop/config.yaml`** — only two fields touched: `fitness.baseline_dir` (placeholder → `eval_interactive/results/m-auto-1b-baseline-20260529`) and `fitness.scoring_code_baseline_sha` (`5177b674…` → `22548e20…`). No other config block change; in particular the `anti_hardcode.synonym_map_enabled: true` from S-Auto-5 Path A and the rule_count / dataset / shadow / lessons / gaming sections UNCHANGED.
- **`autoloop/tests/test_eval_runner.py`** — 5 new tests + 2 updated existing tests (test names in dev handoff §4 table). All PASS in `cd autoloop && uv run --extra dev pytest -q`.
- **`eval_interactive/eval_interactive/case_spec/loader.py`** — the file with Blocker C fix. Specifically: `directory.glob("*.yaml")` → `directory.rglob("*.yaml")` AND an `_is_case_spec(p)` filter that skips `p.name.startswith("_")` (manifest convention). No other change to the loader. The eval_interactive pytest baseline `486 PASS, 3 FAIL` UNCHANGED (the 3 pre-existing failures are env-specific per OQ-S47.3).
- **`autoloop/autoloop/sandbox/anti_hardcode_check.py`** — read-only sample (DO NOT verify edits; the detector is hard-fenced in S-Auto-7). The S-Auto-5 calibrated detector + `synonym_map_enabled: true` Path A toggle + Fix-C step 1 word-boundary regex UNCHANGED.
- **`autoloop/autoloop/loop.py`** + **`autoloop/autoloop/meta_agent/`** + **`autoloop/autoloop/memory/`** + **`autoloop/cli.py`** — read-only sample (DO NOT verify edits; all hard-fenced in S-Auto-7).
- **`eval_interactive/eval_interactive/cli.py`** — read-only sample for fix-path (b) confirmation (must be byte-identical to S-Auto-6 close; fix-path (a) which would have added `--output-dir` to the CLI was NOT taken). `git diff --stat b6084f9..HEAD -- eval_interactive/eval_interactive/cli.py` returns empty.
- **`eval_interactive/results/m-auto-1b-baseline-20260529/`** — the blessed baseline directory. Spot-check the three per-suite symlinks resolve (`ls -lL eval_interactive/results/m-auto-1b-baseline-20260529/`) and each per-suite `results.json` is non-empty with `case_results[]` entries.

Do **not** re-read the milestone-level hard fences in full from `docs/milestone_objective.md` for letter-of-fence verification — they are embedded below.

---

## Embedded sub-sprint contract (cumulative scope claim)

### Goal of S-Auto-7 (from `docs/sprint_objective.md` live during sub-sprint — embedded verbatim per §9 invariant)

S-Auto-7 close 时:

1. **Blocker B resolved (eval_runner fix path (b) — must land FIRST to enable Blocker A blessing).** `autoloop/autoloop/scoring/eval_runner.py:99-110` modified: REMOVE the `--output-dir` argument from the subprocess invocation; adapt `run_v1_fitness_suite` to locate eval-interactive's auto-timestamped output directory; symlink staging at `suite_link` to preserve historical downstream-consumer contract. The `eval_interactive/eval_interactive/cli.py` is UNCHANGED (Blocker B fix path (a) NOT taken; smaller blast radius preserved).
2. **scoring_code_baseline_sha rebaselined.** `_compute_scoring_code_sha()` invoked post-edit; `autoloop/config.yaml` `fitness.scoring_code_baseline_sha` updated from M-Auto-1A close `5177b674…` to the new content hash. `_check_scoring_code_drift(config=config)` returns `[]` silent. Controlled fence #13 override with documented re-baselining.
3. **Blocker A resolved — bless concrete baseline_dir.** Run the v1 47-case fitness suite ONCE on `auto-loop-branch` HEAD with the repaired eval_runner: `bad_cases ×12 (parallel=1) + anchor_outcome ×12 (parallel=4) + shadow ×23 (parallel=4)`. Bless the resulting eval-interactive auto-timestamped directory as the M-Auto-1B baseline by updating `autoloop/config.yaml` `fitness.baseline_dir` from the literal `<PLACEHOLDER-set-at-M-Auto-1A-close>` string to the concrete path. Layer 3 improvement-threshold gate becomes meaningful AND Layer 4 shadow regression `shadow_max_drop_pct: 3.0%` denominator becomes non-zero.
4. **Smoke iter end-to-end verification.** Run `python -m autoloop run --experiments 1` (NO `--dry-run`) on `auto-loop-branch` against the now-blessed baseline + repaired eval_runner. Step 7 (eval_runner) AND Step 9 (tier_evaluator) must reach non-degenerate outputs; `tier_evaluator_verdict` Layer 0-4 outcomes non-degenerate; per-iter elapsed time recorded.
5. **Per-sub-sprint Codex `pass`** (§4.3 trigger #3). Deliver-agent dispatches at S-Auto-7 close (NOT at open) with `compact/sprint-060-codex-review-prompt.md` self-contained per §9 invariant.

### Three human-locked D-clauses (carried forward from M-Auto-1A S-Auto-4 — UNCHANGED in S-Auto-7)

- **D1 — Detector mechanism**: regex + heuristic ONLY. NO second LLM in propose-stage critical path. `FLAG_FOR_CODEX` is a human / Codex review signal — NOT auto-promoted to FAIL and NOT auto-demoted to PASS at propose-stage. *S-Auto-7 does not touch the detector; D1 invariant holds trivially.*
- **D2 — Detector pattern scope**: detector encodes generic structural patterns ONLY (allowed: IF/WHEN/THEN/ELSE decision-tree shapes, `cs<id>`-like tokens, MUST/NEVER phrasing structures, `.contains(...)` literals; forbidden: specific eval case wording, specific user utterances, specific expected assistant answers, known case success/failure labels). *S-Auto-7 does not touch the detector; D2 invariant holds trivially.*
- **D3 — `gaming.py` input contracts** (UNCHANGED in S-Auto-7; gaming.py is hard-fenced #13; the S-Auto-7 `eval_runner.py` controlled override is a SEPARATE file in the fence #13 group).

### What actually shipped in S-Auto-7 (cumulative scope summary)

From dev's handoff (`docs/sprints/sprint-060-handoff.md` §§1-13) and the two-commit diff:

1. **Commit 1 (`559927a`) — Blocker B fix path (b) + scoring SHA rebaseline + tests**. `eval_runner.py` +88/−16 LOC: removes `--output-dir` from subprocess invocation; adds `_locate_new_results_dir` helper with set-diff + mtime tiebreaker; symlink staging at `suite_link.symlink_to(...)` with defensive cleanup. `autoloop/config.yaml` SHA field only: `5177b674…` → `22548e20…` with inline annotation. `autoloop/tests/test_eval_runner.py` +5 new tests + 2 updated existing tests.

2. **Commit 2 (`b0a3704`) — Blocker A baseline_dir blessing + Blocker C loader fix + handoff**. `autoloop/config.yaml` `baseline_dir` field only: placeholder → `eval_interactive/results/m-auto-1b-baseline-20260529` with 6-line annotation naming the three run-IDs (bad_cases `20260529-101324`, anchor_outcome `20260529-102054`, shadow `20260529-102949`), per-case counts (`bad_cases 5/12 + anchor_outcome 7/12 + shadow 4/22 → 16/46 = rate 0.348`, tier-2 mandatory FAILs `18`), and the shadow=22 (NOT 23) note. **In-session Blocker C fix**: `eval_interactive/eval_interactive/case_spec/loader.py` +14/−4: `directory.glob` → `directory.rglob` + new `_is_case_spec(p)` filter skipping `p.name.startswith("_")` (manifest convention). New `docs/sprints/sprint-060-handoff.md` +594 LOC.

3. **Baseline blessing run-IDs + counts**:

   | Suite | Path | Parallel | Run-ID | Wall-clock | total | case_passed | tier2 mandatory FAILs |
   |-------|------|----------|--------|-----------:|------:|------------:|----------------------:|
   | `bad_cases` | `eval_interactive/case_specs/bad_cases/` | 1 | `20260529-101324` | 7m 23s | 12 | 5 (0.417) | 4 |
   | `anchor_outcome` | `eval_interactive/case_specs/anchor_outcome/` | 4 | `20260529-102054` | 1m 50s | 12 | 7 (0.583) | 4 |
   | `shadow` | `eval_interactive/case_specs_shadow/` | 4 | `20260529-102949` | 3m 33s | 22 | 4 (0.182) | 10 |
   | **Total** | — | — | — | **~12m 46s** | **46** | **16 (0.348)** | **18** |

   Total expected was 47 per dev prompt; actual 46 because the 23rd shadow entry was `case_specs_shadow/_manifest.yaml` (manifest metadata, not a case_spec — correctly filtered by the in-session Blocker C `_is_case_spec` filter).

4. **Goal #4 BLOCKED**: smoke iter exp-6 reached Step 6 applier; alt-port Spring spawn `rc=1` at 90.4s. Diagnostic: foreground `:8080` backend (PID 8613) competing with alt-port `:59771` for Flyway migration lock / maven `target/` classpath / Redis pool. This is the OQ-S58.7 1/3-success-rate carryover from S-Auto-5; STOP-and-surfaced per dev prompt as OQ-S60.7 / fix-iteration S-Auto-7.1 candidate. **NOT a Blocker A/B failure**: substrate fixes independently verified via test suite + real-CLI baseline run + `baseline_loader.load` 0-warnings.

5. **Tests** (handoff §10): autoloop pytest `223 → 228 PASS, 1 warning` (+5 new test_eval_runner tests). eval_interactive pytest `486 PASS, 3 FAIL UNCHANGED` (OQ-S47.3 env-specific failures + the in-session loader fix preserves all existing test behaviour). 17-fixture detector sweep `31 PASS UNCHANGED` (not exercised again; S-Auto-7 does not touch the detector).

6. **Adversarial spot-check pre-Codex** (handoff §8): 5 `git diff --stat 1943ed5..HEAD --` checks — fence #13 scoring siblings UNCHANGED ✅, eval-interactive CLI UNCHANGED ✅, Java zero-touch ✅, resources zero-touch (skills lands at S-Auto-8 only) ✅, case_specs / case_specs_shadow zero-touch ✅. Controlled overrides documented in §8: `eval_runner.py` per planning blessing, `loader.py` per in-session human authorization.

### Embedded M-Auto-1B hard fences (verbatim from `docs/milestone_objective.md` §6)

All 17 fences. Codex hard-fence walk uses this list as the canonical reference:

1. **No edits** to `server/src/main/java/**`. Runtime byte-identical to M-Auto-1A close `b6b627b` = M5 close `c9390dc`.
2. **No edits** to `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. Evaluator byte-identical. **S-Auto-7 IN-SESSION OVERRIDE on `eval_interactive/eval_interactive/case_spec/loader.py`** — Blocker C; substrate path-handling repair; in-session human-authorized; NOT pre-blessed at planning round. See Axis B.
3. `server/src/main/resources/skills/*.yaml` conditionally writable EXACTLY ONCE during S-Auto-8 cherry-pick (NOT S-Auto-7). `{prompts,scripts,config,mock}/**` byte-identical.
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, `docs/teams/**`.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-058-*` (sprint-059 was already archived at S-Auto-6 close; sprint-060 handoff is a NEW archive in scope per §6 fence #5 carve-out).
6. **No `git add -A`** by dev. Stage only S-Auto-7 scope files explicitly. Dev followed this discipline per two-commit pattern.
7. Cherry-pick to main **ALLOWED EXACTLY ONCE** during S-Auto-8 (NOT S-Auto-7). Verify no Skill YAML edit.
8. **No new Tier-0 invariant**. C2/C3 DEFER continues.
9. **No cross-file diff** by meta-agent; not applicable in S-Auto-7 because no propose-stage hypothesis is committed to main (the exp-6 hypothesis crashed at Spring spawn before any apply step; the `autoloop/exp-6` branch is left for inspection per applier convention but not merged).
10. **No shadow-set leakage to meta-agent**. Aggregate only. Not exercised in S-Auto-7 (no completed iteration produced a shadow signal yet).
11. **No mutation of `eval_interactive/results/` schema**. Verify: the in-session loader fix does NOT alter the on-disk schema; it only changes HOW the loader walks the directory tree.
12. **No editing of `docs/codex-findings.md` during M-Auto-1B execution**. (Codex writes at S-Auto-7 close per §4.3 trigger #3.)
13. **No modification of `autoloop/autoloop/scoring/` baseline files** (`tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`); content-hash locked at `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`. **S-Auto-7 CONTROLLED OVERRIDE on `eval_runner.py` ONLY** — pre-blessed at planning round 2026-05-29 via in-place milestone_objective.md revision; `scoring_code_baseline_sha` re-baselined to `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`. The other three files (`tier_evaluator.py` / `baseline_loader.py` / `gaming.py`) stay byte-identical. See Axis B.
14. **No hardcoding of any specific eval case wording / user utterance / expected answer / case-status label** in any detector rule. The detector self-discipline 3 regression tests continue to PASS. *S-Auto-7 does not touch the detector; D2 / fence #14 holds trivially.*
15. **No LLM call inside detector or content_validator** (D1). *S-Auto-7 does not touch detector or content_validator.*
16. **No promotion of `gaming.observation_only_in_v1`** from `true` to `false` in M-Auto-1B. *S-Auto-7 does not touch gaming.py.*
17. **At MOST 1 cherry-pick** during S-Auto-8 (redundant with #7). *Not applicable in S-Auto-7.*

### Embedded M-Auto-1B §6.1 OQ-S56.1 disposition (inherited from M-Auto-1A; UNCHANGED in S-Auto-7)

The blessing for `eval_interactive/eval_interactive.yaml` env-var indirection (`bot.base_url: http://localhost:8080` → `${CSAGENT_BACKEND_URL}` + 7-line comment block) carries forward. **In S-Auto-7: no additional edits to that file.** No second-edit surface.

---

## Embedded §4.1 nine-question anti-hardcode kernel (verbatim from `docs/current/anti-hardcode-review-kernel.md`)

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

---

## Verification axes (S-Auto-7 specific — 9 axes; walk in order)

### Axis A — Nine-question kernel walk against substrate-fix scope (scope-exemption hypothesis test)

S-Auto-7 is classified `infra` / `§7 EXEMPT` per the pure-infra carve-out. The §4.1 scope-exemption clause says: "pure infra, docs-only, config-governance, and characterization-test PRs are not subject to this review. If the PR is purely one of those, return `approve` with a one-line note naming the exemption."

**Your task at Axis A is to test the exemption hypothesis**: walk Q1-Q9 against the actual S-Auto-7 cumulative diff. For each question, the expected answer should be "no" / "not applicable" (purely-infra confirmation); if any question surfaces a "yes" or a genuine concern, the scope-exemption claim is invalidated and the verdict must reflect the underlying semantic hardcode concern.

Specifically:

- **Q1**: Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision? Expected: NO. The `eval_runner.py` change is subprocess invocation + filesystem auto-timestamp consumption + symlink staging — pure plumbing. The `loader.py` change is `glob` → `rglob` + skip `_*.yaml` — pure path-handling. The `config.yaml` changes are field updates (SHA + baseline_dir). Verify by sampling the actual diff.
- **Q3**: Could the same outcome be achieved by projecting a soft signal to the LLM? Not applicable — the change does not touch the LLM-projected surface at all; it is substrate-side subprocess + path-walking.
- **Q4**: Does the change encode any visible-eval case text / CaseSpec id / trace-specific phrasing? Expected: NO. Spot-check `autoloop/autoloop/scoring/eval_runner.py` and `eval_interactive/eval_interactive/case_spec/loader.py` for any `cs<id>` / `closure_criterion` / `expected_behavior` / `primary_uc` / `failure_tags` / `source_session_id` literal. The 6-line annotation in `autoloop/config.yaml` for `baseline_dir` names the three run-IDs and per-suite counts — verify this is metadata documentation, NOT a semantic-decision encoding.
- **Q5**: Does the change move semantic ownership from the LLM to Java? Not applicable — no Java edit; no prompt edit; no LLM-projected surface edit.
- **Q7**: Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor? Verify YES via the M-Auto-1B §6 fence walk in Axis G.
- **Q8**: Does the PR ship generalization eval coverage? Substrate-fix doesn't need target/neighbor/negative/shadow case families in the §5.6 sense (no semantic surface modified). Instead, verify the substrate verification (handoff §7 "Independent substrate verification" table): unit-test coverage of new symlink logic, real-CLI confirmation of eval-interactive output convention, clean `baseline_loader.load` consumption, `_check_scoring_code_drift` silent steady state. Judge whether this substrate-verification matrix is the correct analog of generalization coverage for an `infra` sub-sprint.

Report findings inline. If all questions PASS the exemption hypothesis, return `approve` with the one-line exemption note "pure infra; §4.3 trigger #3 fence override controlled + verified". If any question surfaces a concern, escalate to a non-exemption verdict.

### Axis B — Controlled fence overrides verification (fence #13 `eval_runner.py` pre-blessed + fence #2 `loader.py` in-session)

This is the **central axis for §4.3 trigger #3**. Two fence overrides happened in S-Auto-7; both must be verified as genuinely plumbing-only (not smuggling a semantic surface under "infra" framing).

**Override 1 — fence #13 `eval_runner.py` (pre-blessed at planning round)**:

1. Verify the override is documented in `docs/milestone_objective.md` §3 "S-Auto-7 / Sprint 060 — Substrate fix" subsection (in-place revision 2026-05-29 at S-Auto-6 close-empty). The override is named: "CONTROLLED FENCE #13 OVERRIDE: `autoloop/autoloop/scoring/eval_runner.py` (touched per S-Auto-6 close human-locked fix path (b); content-hash rebaselined…)".
2. Verify only `eval_runner.py` is touched in the fence #13 group: `git diff --stat b6084f9..HEAD -- autoloop/autoloop/scoring/tier_evaluator.py autoloop/autoloop/scoring/baseline_loader.py autoloop/autoloop/scoring/gaming.py` returns empty.
3. Verify the modification is genuinely plumbing: subprocess invocation removes a CLI flag that doesn't exist in eval-interactive's CLI (Click would have crashed every overnight iter); adds filesystem auto-timestamp consumption + symlink staging. No regex, no keyword list, no if-else decision branch on case-text, no per-UC matrix. Sample the diff (`eval_runner.py:80-160`) to confirm.
4. Verify the rebaselined `scoring_code_baseline_sha` matches actual `_compute_scoring_code_sha()` output (see Axis C).

**Override 2 — fence #2 `loader.py` (in-session human-authorized; NOT pre-blessed)**:

1. Read handoff §6 "Blocker C (in-session)" and §8 "Controlled overrides" for the authorization narrative. The dev surfaced shadow suite layout mismatch (`case_specs_shadow/` nested under `case_families/<family>/` + `_manifest.yaml` at the top level crashing non-recursive `directory.glob("*.yaml")`); human in-session authorized the substrate path-handling fix via "尽快帮我修复掉吧 / fix it now, get auto-loop running ASAP".
2. Verify the change is genuinely path-handling (NOT a semantic surface):
   - `directory.glob` → `directory.rglob` is recursive directory walk — pure plumbing.
   - `_is_case_spec(p)` filter skipping `p.name.startswith("_")` is a manifest convention (`_*.yaml` is metadata; `cs*.yaml` is case_spec) — pure plumbing.
   - No new decision logic on case content; no semantic interpretation.
3. Verify the change is zero-impact on existing flat case_set dirs: handoff §6 claims "Zero-impact on existing flat case_set dirs (anchor 159 / promotion 101 / exploration 107 / smoke 14 / bad_cases 12 / anchor_outcome 12 — all report identical pre- and post-rglob counts because none contain subdirs or underscore-yamls). Shadow now loads 22 cases." Independently verify by sampling: `eval-interactive run --path case_specs/anchor/` should produce 159 cases (pre-existing flat dir; rglob == glob on flat layouts).
4. Verify eval_interactive pytest 486 PASS / 3 FAIL UNCHANGED (the in-session loader fix preserves all existing test behaviour).
5. **Judge whether the in-session authorization holds**: the dev acted under explicit human in-session authorization, the change is genuinely substrate path-handling (not semantic), and the negative-control coverage (6 flat case_set dirs) is adequate. The override is NOT pre-blessed at planning round (unlike `eval_runner.py`). Should this trigger a follow-up requirement — e.g., a retroactive `milestone_objective.md` §6 fence #2 amendment naming the in-session override — or is the handoff §8 documentation sufficient? If a follow-up is warranted, return `approve with downgrade-to-signal follow-up` with the trigger "deliver-agent + human update `docs/milestone_objective.md` §6 fence #2 with a controlled-override annotation parallel to the fence #13 annotation, at S-Auto-7 close-bundle".

Report findings per override. The verdict tension for Axis B is: both overrides genuinely plumbing-only → `approve`; one or both genuinely plumbing but in-session override needs governance annotation → `approve with downgrade-to-signal follow-up`; one of them smuggles a semantic surface → `reject as semantic hardcode` with §3 layer name.

### Axis C — `scoring_code_baseline_sha` rebaseline reproducibility

Verify the rebaselined `scoring_code_baseline_sha = 22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9` matches actual `_compute_scoring_code_sha()` output post-edit.

Run independently:

```bash
cd autoloop && uv run --extra dev python -c \
  "from autoloop.scoring.gaming import _compute_scoring_code_sha; \
   print(_compute_scoring_code_sha())"
# Expected: 22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9
```

Then verify silent steady state:

```bash
cd autoloop && uv run --extra dev python -c "
import yaml; from pathlib import Path
from autoloop.scoring.gaming import _check_scoring_code_drift
config = yaml.safe_load(Path('config.yaml').read_text())
flags = _check_scoring_code_drift(config=config)
assert flags == [], f'Expected empty drift list; got {flags}'
print('PASS: scoring_code_drift silent')
"
# Expected: "PASS: scoring_code_drift silent"
```

If the hash does not match OR `_check_scoring_code_drift` returns non-empty, return `reject as semantic hardcode` (substrate brittleness — the SHA discipline is itself the §1.7 anti-tampering guard) with §3 layer = `infra`.

If the hash matches AND drift is silent, Axis C PASSes; the fence #13 controlled override is correctly finalized.

### Axis D — Blessed `baseline_dir` spot-check (`eval_interactive/results/m-auto-1b-baseline-20260529/`)

Verify the blessed baseline directory is well-formed for downstream consumption:

1. Spot-check the per-suite symlinks resolve:

   ```bash
   ls -lL eval_interactive/results/m-auto-1b-baseline-20260529/
   # Expected: 3 entries — anchor_outcome, bad_cases, shadow — each pointing to a timestamped dir under eval_interactive/results/
   ```

2. Spot-check each per-suite `results.json` is non-empty with `case_results[]` entries:

   ```bash
   for s in bad_cases anchor_outcome shadow; do
     echo "=== $s ==="
     python -c "import json; r = json.load(open('eval_interactive/results/m-auto-1b-baseline-20260529/$s/results.json')); print('case_count:', len(r.get('case_results', [])))"
   done
   # Expected: bad_cases 12, anchor_outcome 12, shadow 22
   ```

3. Verify `baseline_loader.load` consumes the blessed dir cleanly (reproduce handoff §6 verification):

   ```bash
   cd autoloop && uv run --extra dev python -c "
   import yaml
   from pathlib import Path
   from autoloop.scoring import baseline_loader
   config = yaml.safe_load(Path('config.yaml').read_text())
   baseline_path = Path('../eval_interactive/results/m-auto-1b-baseline-20260529').resolve()
   snap = baseline_loader.load(baseline_path, config=config)
   assert snap.warnings == [], f'Expected zero warnings; got {snap.warnings}'
   for n, s in snap.snapshots.items():
       print(f'  {n}: total={s.total_cases} passed={s.case_passed_count} tier2_fails={s.tier2_mandatory_failure_count}')
   "
   # Expected:
   #   bad_cases: total=12 passed=5 tier2_fails=4
   #   anchor_outcome: total=12 passed=7 tier2_fails=4
   #   shadow: total=22 passed=4 tier2_fails=10
   ```

4. Verify `autoloop/config.yaml:110` `fitness.baseline_dir` advanced from `<PLACEHOLDER-set-at-M-Auto-1A-close>` to `eval_interactive/results/m-auto-1b-baseline-20260529` (or an equivalent absolute-or-repo-relative form).

5. Verify Layer 3 + Layer 4 of `tier_evaluator.evaluate` will now load a non-empty `BaselineSnapshot` (this is the structural goal of the blessing — Layer 3 improvement-min bar no longer trivializes against `... or 0` fallback; Layer 4 `shadow_max_drop_pct: 3.0%` denominator non-zero).

If any spot-check fails, return `approve with downgrade-to-signal follow-up` with trigger = "re-bless the baseline directory at S-Auto-7.1 / S-Auto-8 to recover the missing artefact".

### Axis E — Goal #4 BLOCKED disposition (OQ-S60.7 alt-port Spring spawn)

Goal #4 (smoke iter end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`) is BLOCKED. Per dev prompt: "Smoke iter crashes in unhandled path NOT caused by Blockers A/B: STOP-and-surface. Fix-iteration S-Auto-7.1 candidate." Dev followed this contract exactly.

Read handoff §7 (Smoke iter end-to-end record). Verify:

1. **The smoke crash is upstream of any Blocker A/B substrate fix**: handoff §7 documents exp-6 reached Step 6 applier; alt-port Spring spawn `rc=1` at 90.4s. Steps 7 (eval_runner; Blocker B surface) and Step 9 (tier_evaluator; consumes Blocker A blessed baseline) were structurally unreachable in this iter. The Spring spawn failure is on a DIFFERENT substrate surface (the `applier.py` alt-port spawn step at Step 6) than what S-Auto-7 fixed.
2. **The OQ-S58.7 carryover linkage is sound**: handoff §7 names "the foreground :8080 backend (PID 8613, needed for the §6 baseline run) competes with the alt-port spawn for shared resources during concurrent `spring-boot:run`" — Flyway migration lock contention, maven multi-module `target/` race, Redis pool. This is the same OQ-S58.7 pattern (S-Auto-5 exp-2 Spring spawn failure on port 51786) carried into the post-baseline-run window.
3. **Independent substrate verification compensates for the missing loop-context exercise**: handoff §7 "Independent substrate verification" table lists 5 verifications — autoloop pytest 228 PASS (unit-test coverage of new symlink logic), real-CLI confirmation of eval-interactive auto-timestamp output (three §6 baseline run-IDs), `baseline_loader.load` 0 warnings (Axis D above), `_check_scoring_code_drift` silent (Axis C above), and the synthetic-test of `_locate_new_results_dir` set-diff helper. Judge: is this 5-fold independent verification an acceptable substitute for the loop-context smoke iter, given Goal #4 is BLOCKED upstream?

**Verdict implication**: Goal #4 BLOCKED is a contract-anticipated outcome (dev prompt explicitly named the STOP-and-surface path). The fix-iteration S-Auto-7.1 is the deliver-agent + human's downstream decision. For Codex, the question is: do the independent substrate verifications adequately demonstrate that the S-Auto-7 substrate fixes work, even though the smoke iter is BLOCKED upstream? If YES → `approve` (the substrate-fix scope is complete and verified; OQ-S60.7 is a different substrate surface for S-Auto-7.1). If NO → `approve with downgrade-to-signal follow-up` with trigger = "S-Auto-7.1 must verify the smoke iter end-to-end through Step 9 before M-Auto-1B can dispatch S-Auto-8 overnight".

### Axis F — Test count baselines preserved

Verify the three test baseline claims in handoff §10:

1. autoloop pytest: 223 → 228 PASS, 1 WARN. `+5` new tests in `autoloop/tests/test_eval_runner.py` (`test_eval_runner_removes_output_dir_arg`, `test_eval_runner_locates_auto_timestamped_output_via_set_diff`, `test_eval_runner_returns_suiterunresult_with_correct_paths`, `test_eval_runner_no_new_dir_leaves_results_path_absent`, `test_locate_new_results_dir_picks_newest_by_mtime_on_multi`); 2 updated existing tests no count change.
2. eval_interactive pytest: `486 PASS, 3 FAIL UNCHANGED` (OQ-S47.3 env-specific failures; the in-session loader fix preserves all existing test behaviour).
3. 17-fixture detector sweep: `31 PASS UNCHANGED` (not exercised again; S-Auto-7 does not touch the detector). Detector self-discipline 3 regression tests UNCHANGED — PASS.

Reproduce:

```bash
cd autoloop && uv run --extra dev pytest -q
# Expected: 228 passed, 1 warning

cd eval_interactive && uv run python -m pytest --tb=no -q
# Expected: 486 passed, 3 failed
```

If any count diverges materially, surface as a finding.

### Axis G — Hard-fence verification against M-Auto-1B §6 17 fences (other than fence #13 + #2 controlled overrides)

Run:

```bash
git diff --stat 60c5b67..HEAD -- \
  server/src/main/java/ eval/src/main/java/ \
  eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ \
  data/ db/migration/ \
  server/src/main/resources/ \
  docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/iteration_governance.md docs/teams/
```

(Note: parent ref is `60c5b67` not `1943ed5` because `60c5b67` is the immediate parent of `559927a` — the first S-Auto-7 commit. Using `1943ed5..HEAD` would include the out-of-scope S-Auto-6 deliver close-bundle + the out-of-scope architecture README; Axis I addresses the README.)

Expected: empty output (zero edits to all enumerated gated surfaces).

Spot-check the four `autoloop/autoloop/scoring/` files via Axis C (`_compute_scoring_code_sha` matches the new `22548e20…` hash; the OTHER three files byte-identical to S-Auto-6 close).

Spot-check `eval_interactive/eval_interactive/cli.py` byte-identical (fence #2 controlled override is only on `case_spec/loader.py`, NOT on `cli.py`):

```bash
git diff --stat 60c5b67..HEAD -- eval_interactive/eval_interactive/cli.py
# Expected: empty
```

Spot-check the sprint archives (`docs/sprints/sprint-001-*` through `docs/sprints/sprint-058-*`) — no edits expected. The NEW `docs/sprints/sprint-060-handoff.md` is in-scope per fence #5 carve-out for new archives.

Verify NO Skill YAML edit landed (fence #3 — cherry-pick to skills/ is S-Auto-8 ONLY):

```bash
git diff --stat 60c5b67..HEAD -- server/src/main/resources/skills/
# Expected: empty
```

If any fence verification surfaces an unexpected edit, escalate immediately (likely scope creep beyond the two controlled overrides).

### Axis H — Two-commit pattern verification

Dev's commit pattern matches the dev prompt's `## Commit discipline` two-commit guidance:

- **Commit 1 (`559927a`)**: Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker B fix path (b) + scoring SHA rebaseline + tests. Staged: `autoloop/autoloop/scoring/eval_runner.py`, `autoloop/config.yaml` (SHA only), `autoloop/tests/test_eval_runner.py`.
- **Commit 2 (`b0a3704`)**: Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker A baseline_dir blessing + Blocker C loader fix + handoff. Staged: `autoloop/config.yaml` (baseline_dir only), `eval_interactive/eval_interactive/case_spec/loader.py`, `docs/sprints/sprint-060-handoff.md`.

Verify:

1. The two commits respect the staging discipline (each commit has only its declared file set; no `git add -A` smuggling).
2. The commit messages match the dev prompt §13 templates (commit-1 + commit-2 stanzas).
3. Commit 2 also includes the in-session Blocker C `loader.py` fix — this is the in-session scope expansion documented in §8. The commit message addendum names the addition.
4. No third commit smuggled into the S-Auto-7 range. (The out-of-scope `60c5b67` README commit pre-dates S-Auto-7.)

The two-commit pattern separates the "fence #13 override + rebaseline" structural change (Commit 1) from the "baseline blessing + Blocker C + handoff" verification step (Commit 2), giving a clean audit trail.

### Axis I — Out-of-scope `60c5b67` README commit handling

Between the S-Auto-6 deliver-agent close-bundle (`b6084f9`) and the S-Auto-7 first commit (`559927a`), an out-of-scope commit landed:

- `60c5b67` — "Add comprehensive architecture README for the csagent project." — `README.md` +595 lines.

This commit is **NOT part of S-Auto-7's substrate-fix scope**. It is an ad-hoc documentation addition that happened on the same branch.

Verify:

1. The commit is genuinely out-of-scope and does NOT touch any M-Auto-1B §6 fenced surface. `git show --stat 60c5b67` should show only `README.md` modification.
2. The commit does NOT materially affect the S-Auto-7 substrate-fix verdict (no semantic surface modified; no code path edited; no fence override).
3. Surface this to the deliver-agent + human as a planning observation: was this commit intentional (architecture documentation) or accidental scope creep? If intentional, the deliver-agent should note it in `docs/10-handoff.md` §1 lead at S-Auto-7 close. If accidental, the human should decide whether to leave it or revert.

**Verdict implication**: this is a planning / governance hygiene observation, NOT a Codex blocker. Surface it as a finding under Axis I; the verdict should remain whatever the substrate-fix axes determine.

---

## Output format (§4.2 sprint-close header verbatim)

Write your review to `docs/codex-findings.md` (the live scaffold). Replace the scaffold body with your review content. The deliver-agent will `git mv` the file to `docs/milestones/M-Auto-1B_codex-review.md` at M-Auto-1B close per the standard close-out artefact list; in the interim, the live file carries this S-Auto-7 per-sub-sprint review until the milestone-shared review at M-Auto-1B close appends to it (or replaces it for milestone-shared verdict).

Use this exact 4-line header at the top of your review section (per `iteration_governance.md` §4.2):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the verdict + key axes findings>
```

Followed by per-axis findings (A through I above), each as a labelled subsection with:

- Axis name + one-sentence finding (PASS / FAIL / CONCERN).
- If CONCERN or FAIL: paste diff snippet (≤200 chars) + reasoning + the §3 layer name where the fix should target + the recommended verdict downgrade (if any).

Then issue **exactly one §4.1 verdict** (the four-verdict set):

- `approve` — the change is pure-infra (Axis A scope-exemption hypothesis confirmed) AND both controlled fence overrides are genuinely plumbing-only (Axis B) AND scoring SHA reproduces (Axis C) AND blessed baseline_dir is well-formed (Axis D) AND Goal #4 BLOCKED is contract-anticipated with adequate substrate verification (Axis E) AND test baselines preserved (Axis F) AND hard-fence walk clean (Axis G) AND two-commit pattern respected (Axis H) AND out-of-scope README is documentation hygiene (Axis I). One-line scope-exemption note required per §4.1 clause.
- `approve with downgrade-to-signal follow-up` — the change is acceptable as an interim measure, but a follow-up sprint must (a) deliver-agent + human update `docs/milestone_objective.md` §6 fence #2 with a controlled-override annotation parallel to the fence #13 annotation, at S-Auto-7 close-bundle (closes Axis B in-session override governance hygiene gap); OR (b) S-Auto-7.1 must verify the smoke iter end-to-end through Step 9 before S-Auto-8 overnight can dispatch (closes Axis E Goal #4 BLOCKED if Codex judges independent substrate verification insufficient); OR (c) the out-of-scope `60c5b67` README commit needs explicit human acknowledgment in §1 lead (closes Axis I governance hygiene). Name the trigger.
- `reject as semantic hardcode` — the change encodes a soft semantic decision the LLM should own (e.g., Codex judges that the `eval_runner.py` or `loader.py` change smuggles a semantic surface under "infra" framing; or the scoring SHA does NOT reproduce; or the blessed baseline_dir is materially malformed). Name the §3 layer the fix should target.
- `needs human architecture decision` — the change crosses an unresolved governance question (e.g., whether in-session fence overrides require retroactive planning-round annotation; whether the §4.3 trigger #3 + §4.1 scope-exemption interaction needs governance-doc clarification).

---

## Constraints

- **Do NOT edit code.** Your review is read-only; you may sample any code path but the only file you write to is `docs/codex-findings.md`.
- **Do NOT re-judge §5.6 bad-case suite verdicts** (no bad-case rerun is in scope for this per-sub-sprint review — that's a milestone-close gate, not a sub-sprint gate; S-Auto-7 is `infra` / §7 EXEMPT so no bad-case rerun was contracted).
- **Do NOT re-litigate the M-Auto-1A close** or the M-Auto-1B S-Auto-5 close (Sprint 057 S-Auto-4 Codex review accepted M-Auto-1A substrate; Sprint 058 S-Auto-5 per-sub-sprint Codex `compact/sprint-058-codex-review-prompt.md` returned `decision: pass / blocking_count: 0` first pass single round 2026-05-28 with verdict `approve with downgrade-to-signal follow-up` — that verdict is settled and is not re-opened here).
- **Do NOT re-litigate the S-Auto-6 close-empty** (Sprint 059 closed empty Class C — In-flight downgrade per `docs/current/deliver_close_taxonomy.md`; the close decision is settled at deliver-agent + human joint AskUserQuestion 2026-05-29; this S-Auto-7 review is the contractual follow-up).
- **Per-sub-sprint trigger** (informational): per `iteration_governance.md` §4.3, per-sub-sprint Codex is REQUIRED when the sub-sprint (1) introduces a Tier-0 candidate; (2) crosses a §1.7 forbidden-list red line; (3) **touches a hard-fenced surface that the milestone objective explicitly named out of scope**; (4) closes a sub-sprint with a `fix_required` outcome that needs per-sub-sprint re-review. S-Auto-7 triggers under (3) — fence #13 controlled override on `eval_runner.py` (pre-blessed at planning round) + fence #2 in-session override on `loader.py` (human-authorized during execution).
- **Out-of-scope-review** verdict (`out_of_scope_review`) is the appropriate verdict if you judge that S-Auto-7's actual scope did NOT cross any §4.3 trigger condition in a way that required per-sub-sprint review. However, given the dev contract explicitly named §4.3 trigger #3 + the two fence overrides are documented in handoff §8, this verdict is unlikely.
- **Verdict must return BEFORE S-Auto-8 overnight starts.** If you return `reject as semantic hardcode`, a fix-iteration sub-sprint S-Auto-7.1 is required before S-Auto-8 can dispatch (and S-Auto-7.1 may need to also address the Goal #4 BLOCKED Spring spawn issue per Axis E).
- **S-Auto-7 close classification** (A / B / C / D per `docs/current/deliver_close_taxonomy.md`) is the deliver-agent + human's downstream decision, NOT a Codex output. Your verdict feeds that classification (`pass` → most likely A or B-with-fix-iteration-S-Auto-7.1; `approve with downgrade-to-signal follow-up` → B; `reject` → B with re-spin; `out_of_scope_review` → A-with-OOSR or escalation). Do not classify the close yourself.

---

## Pre-mitigation already in place (dev-side; reduces Codex friction)

From handoff §1, §4-§8, §10:

- §1 §7 self-walk explicitly walks Target failure layer (`infra`) / Tier-0 invariant (none added) / Semantic hardcode (none introduced; rationale per change) / Generalization coverage (target+neighbor+negative-control+shadow per substrate-fix analog).
- §4 Blocker B fix evidence: 5 new tests + 2 updated existing tests; before/after diff with key lines; direct subprocess invocation result; SuiteRunResult shape post-edit.
- §5 scoring_code_baseline_sha rebaseline evidence: old hash + new hash + reproducibility command + silent steady state confirmation + fence #13 verification.
- §6 Baseline blessing evidence: three eval-interactive run-IDs; per-suite case_passed counts; tier2 mandatory FAILs; Blocker C in-session fix narrative + zero-impact negative-control on 6 flat case_set dirs; baseline_loader verification output reproduced.
- §7 Smoke iter end-to-end record: Goal #4 BLOCKED disposition with diagnostic root-cause (Flyway lock contention / maven race / Redis pool) + independent substrate verification table compensating for the missing loop-context exercise.
- §8 Adversarial spot-check pre-Codex: 5 `git diff --stat` checks against M-Auto-1B §6 fences (other than the two controlled overrides) — all empty.
- §10 Test counts: 228 passed, 1 warning autoloop (baseline 223 + 5 new); eval_interactive 486 passed, 3 failed UNCHANGED.

This means Axes A / C / D / F / G / H are likely PASS without further dev work. The verdict tension lives in Axes B (Blocker C in-session override governance hygiene), E (Goal #4 BLOCKED → fix-iteration S-Auto-7.1 sufficiency of independent substrate verification), and I (out-of-scope `60c5b67` README commit governance hygiene).

---

**END OF CODEX REVIEW PROMPT.** Begin with Axis A. Walk each axis in order. Write your review to `docs/codex-findings.md` using the §4.2 sprint-close header verbatim + per-axis findings + exactly one §4.1 verdict.
