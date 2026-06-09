---
title: Sprint 48 / S-Cleanup-2 handoff — Bad-case fixture schema unification + executor suite-mode
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff for Sprint 48 / S-Cleanup-2)
last_reviewed: 2026-05-23
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Dev handoff for Sprint 48 / S-Cleanup-2 (second sub-sprint of
  Milestone M4-Eval-Cleanup). §12 closure verdict reserved for
  deliver-agent + human at sub-sprint close.
---

# Sprint 48 / S-Cleanup-2 handoff — Bad-case fixture schema unification + executor suite-mode

## 1. Identity

- **Sprint number**: 48 (global) / S-Cleanup-2 (M4-Eval-Cleanup sub-sprint 2).
- **Milestone**: M4-Eval-Cleanup (`docs/milestone_objective.md`).
- **Branch**: `refactor/remove-the-shackles`.
- **HEAD prior to dev**: `1576070` (S-Cleanup-1 dev commit; deliver-agent close-bundle commit on top of `4344662` per `docs/10-handoff.md` §0).
- **Dev commit SHA**: appended at commit time (see §11 R-item flip request + bundle policy §7).

## 2. Scope landed

Per `docs/sprint_objective.md` §4. Numstat is from `git diff --numstat <S2-files>` against HEAD `1576070` (same shape as `git show --numstat <commit>` post-commit).

| Item | File | insertions | deletions | Notes |
|------|------|------------|-----------|-------|
| #5 | `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` | 2 | 12 | Strip Alice's legacy `outcome_checks` (7 → empty) + `llm_judge_dimensions` (3 → empty) per M3-Eval new-schema convergence intent. All 6 mandatory preserve fields UNCHANGED (`closure_criterion`, `bad_case_metadata.{surfaced_by, surfaced_date, failure_shape, layers_involved, related_dimensions, related_r_items}`; top-level `source_session_id`); all other CaseSpec fields (`expected`, `persona`, `form_context`, `scoring.hard_checks`) UNCHANGED. |
| #2 | `eval_interactive/eval_interactive/case_spec/schema.py` | 10 | 0 | NEW `CaseSpec.source_suite: Optional[str] = None` field carrying the suite directory name the case was loaded from. Backward-compat default `None` for direct-instantiation in unit tests. |
| #2 | `eval_interactive/eval_interactive/case_spec/loader.py` | 27 | 6 | `_parse_case_spec` + `load_case_spec` + `load_case_specs` take optional `source_suite` parameter; default inference uses `path.parent.name` / `directory.name`. |
| #2 | `eval_interactive/eval_interactive/batch/sets.py` | 26 | 0 | NEW public helper `is_human_judgment_suite(suite_name: str \| None) -> bool` centralising the `_OPT_IN_SETS` membership test (None / unknown → False). |
| #2 | `eval_interactive/eval_interactive/batch/executor.py` | 88 | 6 | NEW module-level `_resolve_case_passed_authority(case_spec)` returning `"human_review"` (for opt-in suites) / `"programmatic"` (default). `_build_case_result` + `_timeout_result` + `_error_result` + `_contract_violation_result` all emit `case_passed_authority` on the result dict. `run_batch` prints a `Suite type: human_judgment ... §5.6 ...` banner when any case in the run originates from an opt-in suite. Per-case stdout in `_execute_case_sync` renders a `HUMAN_REVIEW {case_id} (programmatic={PASS/FAIL}; human review of closure_criterion required per §5.6) ...` line instead of the `PASS`/`FAIL` line when authority = human_review. All call sites use `getattr(case_spec, "source_suite", None)` so legacy mock specs (e.g., the `_Spec` stand-in in `tests/trace/test_contract_validation.py`) keep working. |
| #2 | `eval_interactive/tests/test_case_passed_authority.py` | 349 (new file) | 0 | NEW test module — 17 tests covering: `is_human_judgment_suite` membership (3 tests), `_resolve_case_passed_authority` for opt-in / programmatic / None / unknown suites (5 tests), `load_case_spec` parent-dir inference + explicit override (4 tests), `_build_case_result` + 3 placeholder builders emit authority (5 tests). |

**Total S2-scope mutations** (modified files only, via `git diff --shortstat <S2-files>`): **5 files changed, +153 insertions, -24 deletions**.
**Plus NEW test file** `tests/test_case_passed_authority.py`: **349 lines**.

Reproducible commands:

```bash
git diff --numstat \
  eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml \
  eval_interactive/eval_interactive/batch/executor.py \
  eval_interactive/eval_interactive/batch/sets.py \
  eval_interactive/eval_interactive/case_spec/loader.py \
  eval_interactive/eval_interactive/case_spec/schema.py
git diff --shortstat <same paths>
wc -l eval_interactive/tests/test_case_passed_authority.py
```

**Files NOT staged** (deliver-agent / human bundle at launch commit per `iteration_governance.md` §8.7):

- `docs/milestone_objective.md` (deliver-agent)
- `docs/sprint_objective.md` (deliver-agent)
- `docs/10-handoff.md` (deliver-agent)
- `docs/current/doc_governance.md` (deliver-agent close bundle artefact)
- `docs/current/iteration_governance.md` (deliver-agent close bundle artefact)
- `docs/current/deliver_close_taxonomy.md` (deliver-agent — untracked)
- `compact/sprint-deliver-orchestrator.md` (deliver-agent template — untracked working-tree mod)
- `compact/sprint-047-dev-prompt.md` / `compact/sprint-048-dev-prompt.md` (deliver-agent — untracked)
- `compact/deliver-activation.md` (deliver-agent — untracked)

## 3. §3 layer-classification + anti-hardcode self-walk

The §7 stanza in `docs/sprint_objective.md` §3 holds against the actual commit:

- **Target failure layer**: `eval_spec` (primary — Alice fixture schema strip #5) + `infra` (secondary — executor suite-mode annotation #2).
- **Tier-0 invariant**: This sprint adds no Tier-0 invariant. `docs/runtime_freeze_and_risk_policy.md` §1 / §2 UNTOUCHED (confirmed below in §10 hard-fence checklist).
- **Semantic hardcode**: No semantic hardcode introduced. (a) Stripping Alice's `outcome_checks` + `llm_judge_dimensions` is a fixture-data edit on a single YAML reflecting M3-Eval design-intent alignment; it removes data, not logic. (b) The `case_passed_authority` field is a report metadata flag determined by the suite directory name (a configuration tuple `_OPT_IN_SETS`), NOT by the semantic content of any case; the underlying composite / `case_passed` / scoring decisions are unchanged.
- **Generalization coverage**: Not applicable — this sub-sprint adds no new semantic surface; per-item verification (§6) is the acceptance approach.

### §4.1 nine-question kernel self-walk

Per `iteration_governance.md` §4.1, walked against the S2 staged diff:

1. **Q1 (keyword / regex / if-else / enum / per-UC matrix for semantic decision)** — NO. `_resolve_case_passed_authority` branches on directory-name membership in `_OPT_IN_SETS` (a configuration tuple of suite names from S-Cleanup-1); the suite name is not a semantic decision and the field is informational metadata that does not change `case_passed` / composite gates.
2. **Q2 (justified as protecting Tier-0)** — N/A (no Q1 yes).
3. **Q3 (could be projected as soft signal instead)** — N/A (Q1 = no). The `case_passed_authority` field IS a soft observability annotation by construction; it surfaces existing §5.6 governance state on the per-case result row rather than encoding a new decision in Java / prompt.
4. **Q4 (encodes visible-eval case text / trace phrasing / CaseSpec id)** — NO. `_resolve_case_passed_authority` reads only the suite directory name (`bad_cases` / `anchor_outcome` / `anchor` / ...); no CaseSpec id, no trace text, no case-specific keyword. Alice's strip removes data without adding any.
5. **Q5 (moves semantic ownership LLM → Java)** — NO. The LLM-side semantic surface is untouched. `server/src/main/java/**`, `server/src/main/resources/system_prompt.txt`, and Skill YAMLs are all UNTOUCHED. No new Java decision was added.
6. **Q6 (adds if-else block to prompt)** — NO. `server/src/main/resources/system_prompt.txt` UNTOUCHED.
7. **Q7 (preserves tool schema / capability / PII / grounding floor)** — YES (preserved). Tool schema, capability boundary, PII floor, grounding floor are all server-side; S2 makes no `server/` changes.
8. **Q8 (ships target / neighbor / negative / shadow coverage)** — N/A (no new semantic surface per §3 stanza). The 17 NEW tests in `tests/test_case_passed_authority.py` cover the infra invariants (opt-in membership, default → programmatic for None / unknown, 4 result-builder code paths).
9. **Q9 (rollback plan if temporary)** — N/A (changes are durable schema additions and fixture migrations, not temporary measures). `git revert <s2-commit>` is the rollback path; the `source_suite=None` default on `CaseSpec` means rollback does not break any direct CaseSpec instantiation in tests.

**Verdict (self-assessment)**: `approve`. Clean §4.1 pass per Q1-Q9 because S2 is fixture migration + infra annotation (no semantic hardcode introduced; no semantic decision moved LLM → Java; no forbidden primitive); no question yielded a non-trivial "yes" or "concern".

## 4. Java baseline

`cd server && mvn test -q` (no flags):

```
[ERROR] Failures:
[ERROR]   SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53
    ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor ==> expected: <true> but was: <false>
[ERROR] Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2
```

**Baseline preservation**: `1163 / 1-inherited / 0 / 2` — IDENTICAL to S-Cleanup-1 close (`1576070`) and M3-Eval close (`7562a2d`) per `docs/10-handoff.md` §0. The 1 inherited failure persists per OQ-S41.5 STATUS QUO. S2 touches NO `server/` code (eval-side fixture + infra annotation only).

Reproducible command: `cd server && mvn test -q 2>&1 | tail -3`.

## 5. Python baseline

`cd eval_interactive && uv run pytest --tb=no -q` (full suite):

**Pre-S2** (S2 changes stashed; new test file moved aside via `mv tests/test_case_passed_authority.py /tmp/...`):

```
7 failed, 424 passed in 11.21s
```

**Post-S2** (S2 changes restored; new test file in place):

```
7 failed, 441 passed in 11.15s
```

**Net delta from S2 (dev environment)**: **+17 passed, +0 failed** (17 NEW tests in `tests/test_case_passed_authority.py` all PASS; ZERO new failures introduced).

The 7 baseline failures are pre-existing (identical to S-Cleanup-1 close §5 in this dev env per `docs/sprints/sprint-047-handoff.md` §5 — `test_case_spec_overrides.py` × 2 + `test_corpus_lint.py` × 5 from the env-specific `ModuleNotFoundError: No module named 'eval_interactive.case_spec'` subprocess issue per OQ-S47.3 disposition).

**Deliver-agent environment projection**: per `docs/10-handoff.md` §0, the deliver-agent's documented Python baseline post-S-Cleanup-1 is `3 failed, 428 passed` (5 → 3 from S1 Item #8). S2 introduces 0 new failures and 17 new passes → projected deliver-agent count post-S2: `3 failed, 445 passed`.

Reproducible commands:

```bash
# Pre-S2
git stash push eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml \
  eval_interactive/eval_interactive/batch/executor.py \
  eval_interactive/eval_interactive/batch/sets.py \
  eval_interactive/eval_interactive/case_spec/loader.py \
  eval_interactive/eval_interactive/case_spec/schema.py
mv eval_interactive/tests/test_case_passed_authority.py /tmp/
cd eval_interactive && uv run pytest --tb=no -q 2>&1 | tail -1
# Post-S2
mv /tmp/test_case_passed_authority.py eval_interactive/tests/
cd /Users/caoruixin/projects/csagent-latest && git stash pop
cd eval_interactive && uv run pytest --tb=no -q 2>&1 | tail -1
```

## 6. Per-item verification evidence

### Item #5 — Alice bad-case fixture strip

**Strip verification** (`grep -n` on the live YAML):

```
$ grep -n "^outcome_checks:\|^llm_judge_dimensions:\|  outcome_checks:\|  llm_judge_dimensions:" \
    eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
146: outcome_checks: []
147: llm_judge_dimensions: []
```

Both lists empty per S-Eval-4 11-case design-intent schema.

**Preserve verification** (closure_criterion + load-bearing metadata UNCHANGED):

```
$ grep -A 1 "^closure_criterion:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
closure_criterion: |
  Bot reaches one of these end-states without entering a multi-turn
...
$ grep -A 25 "^bad_case_metadata:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
bad_case_metadata:
  surfaced_by: human (Alice mock session via real-LLM run; reported in deliver-agent investigation 2026-05-16)
  surfaced_date: 2026-05-16
  original_session_id: 3772e56b-caa7-4e0a-84fc-75a26ffbe2b2
  failure_shape: |
    UC-A user (listing visibility / "why was my ad removed") gets
    mis-classified to UC-H (Ad Removal Appeal) at DISCOVER phase
    ... [identical to pre-S2; 14-line failure_shape block UNCHANGED]
  layers_involved: prompt_projection ...; skill_state ...; java_guard ...; semantic_planner ...
  related_dimensions: D1 (mis-classification), D2 (intake prefill gap), D3 (INTAKE lock-in escape), D4 (stated_reason circularity)
  related_r_items:
    - R-option-beta-coverage-gap-uc-a-uc-c-shape (Sprint 32; ...)
    - R-llm-provider-latency-drift-2026-05-16 (Sprint 31; ...)
```

All 6 preserve-mandatory fields UNCHANGED (the dev-prompt's `expected_behavior` name maps to Alice's `failure_shape` + `expected.bot_handling_pattern` content — both preserved). Top-level `source_session_id: synthetic-bad-case-alice-uc-a-uc-h-2026-05-16` (line 2) UNCHANGED. `expected.bot_handling_pattern` (the qualitative bot-pattern narrative the dev-prompt called out as the "expected_behavior" equivalent) UNCHANGED (lines 97-113).

**End-to-end load verification**:

```
$ cd eval_interactive && uv run python -c \
  "from eval_interactive.case_spec.loader import load_case_spec; \
   from pathlib import Path; \
   cs = load_case_spec(Path('case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml')); \
   print(cs.case_id); \
   print('outcome_checks:', cs.scoring.outcome_checks); \
   print('llm_judge_dimensions:', cs.scoring.llm_judge_dimensions); \
   print('primary_uc:', cs.expected.primary_uc); \
   print('secondary_ucs:', cs.expected.secondary_ucs)"
alice_uc_a_uc_h_misclass
outcome_checks: []
llm_judge_dimensions: []
primary_uc: UC-A
secondary_ucs: ['UC-FP']
```

Case still loads end-to-end; no schema validation error; `expected.primary_uc` + `secondary_ucs` preserved.

### Item #2 — Executor suite-mode annotation

**Authority resolution unit verification** (no server roundtrip needed):

```
$ uv run python -c \
  "from eval_interactive.batch.executor import _resolve_case_passed_authority; \
   from eval_interactive.case_spec.loader import load_case_spec; \
   from pathlib import Path; \
   for p in ['case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml', \
             'case_specs/anchor_outcome/anchor_outcome_uc_a_visibility.yaml', \
             'case_specs/anchor/cs_interactive_001.yaml']: \
     cs = load_case_spec(Path(p)); \
     print(f'{p} -> source_suite={cs.source_suite!r} authority={_resolve_case_passed_authority(cs)!r}')"
case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml -> source_suite='bad_cases' authority='human_review'
case_specs/anchor_outcome/anchor_outcome_uc_a_visibility.yaml -> source_suite='anchor_outcome' authority='human_review'
case_specs/anchor/cs_interactive_001.yaml -> source_suite='anchor' authority='programmatic'
```

**End-to-end smoke against live bot** (bad_cases via `--set bad_cases --parallel 1 --limit 2`; `results/20260523-024300/results.json`):

```
$ jq '.case_results[] | {case_id, case_passed_authority}' results/20260523-024300/results.json
{
  "case_id": "alice_uc_a_uc_h_misclass",
  "case_passed_authority": "human_review"
}
{
  "case_id": "cs001_uc_c_mechanical_template_escalate",
  "case_passed_authority": "human_review"
}
```

**End-to-end smoke against live bot** (anchor via `--path case_specs/anchor/cs_interactive_001.yaml`; latest `results/<run_id>/results.json`):

```
$ jq '.case_results[0] | {case_id, case_passed_authority}' results/<latest>/results.json
{
  "case_id": "cs_interactive_001",
  "case_passed_authority": "programmatic"
}
```

**Stdout banner + per-case annotation verification** (bad_cases run):

```
Starting batch run 's-cleanup-2-bad-cases-banner' -- 1 case(s), parallel=1
  Suite type: human_judgment (per iteration_governance.md §5.6); programmatic PASS/FAIL is informational only — manual review of closure_criterion against per_turn_trace is the gate.
  HUMAN_REVIEW alice_uc_a_uc_h_misclass  (programmatic=FAIL; human review of closure_criterion required per §5.6)  composite=0.000  turns=4  stop=bot_ended
```

**Stdout for anchor run (banner absent, regular PASS/FAIL line)**:

```
Starting batch run 's-cleanup-2-anchor-banner' -- 1 case(s), parallel=1
  FAIL    cs_interactive_001  composite=0.000  turns=4  stop=goal_achieved
```

The `Suite type: human_judgment ...` banner does NOT appear for the anchor run; the per-case line uses the original `FAIL` shape — programmatic path preserved unchanged.

**NEW tests for Item #2** (`tests/test_case_passed_authority.py`, 17 tests):

```
$ uv run pytest tests/test_case_passed_authority.py -v 2>&1 | tail -22
tests/test_case_passed_authority.py::test_is_human_judgment_suite_recognises_opt_in_suites PASSED
tests/test_case_passed_authority.py::test_is_human_judgment_suite_rejects_known_programmatic_suites PASSED
tests/test_case_passed_authority.py::test_is_human_judgment_suite_rejects_none_and_unknown PASSED
tests/test_case_passed_authority.py::test_resolve_authority_returns_human_review_for_bad_cases PASSED
tests/test_case_passed_authority.py::test_resolve_authority_returns_human_review_for_anchor_outcome PASSED
tests/test_case_passed_authority.py::test_resolve_authority_returns_programmatic_for_anchor PASSED
tests/test_case_passed_authority.py::test_resolve_authority_defaults_to_programmatic_when_suite_is_none PASSED
tests/test_case_passed_authority.py::test_resolve_authority_defaults_to_programmatic_for_unknown_suite PASSED
tests/test_case_passed_authority.py::test_load_case_spec_infers_source_suite_for_bad_cases PASSED
tests/test_case_passed_authority.py::test_load_case_spec_infers_source_suite_for_anchor_outcome PASSED
tests/test_case_passed_authority.py::test_load_case_spec_infers_source_suite_for_programmatic_anchor PASSED
tests/test_case_passed_authority.py::test_load_case_spec_explicit_override_wins PASSED
tests/test_case_passed_authority.py::test_build_case_result_emits_human_review_for_bad_cases PASSED
tests/test_case_passed_authority.py::test_build_case_result_emits_programmatic_for_anchor PASSED
tests/test_case_passed_authority.py::test_timeout_result_emits_authority PASSED
tests/test_case_passed_authority.py::test_error_result_emits_authority PASSED
tests/test_case_passed_authority.py::test_contract_violation_result_emits_authority PASSED
============================== 17 passed in 0.29s ==============================
```

**Mock-spec compatibility fix**: during full-suite run, the pre-existing `tests/trace/test_contract_validation.py::TestExecutorIntegration` tests use an inline `_Spec` mock object (not a real `CaseSpec`) that does NOT carry a `source_suite` attribute. Initial S2 wiring caused 2 NEW failures (`AttributeError: '_Spec' object has no attribute 'source_suite'`). Fixed by switching to `getattr(case_spec, "source_suite", None)` at all 3 call sites in `executor.py` (the helper resolver + the `run_batch` banner predicate + the `_execute_case_sync` per-case echo). The 2 contract-validation tests then pass without modification because the defensive `None` default routes mock-spec cases to `"programmatic"` (the safe default).

### Alice regression-safety smoke

The 2-case `bad_cases --limit 2` smoke (`results/20260523-024300/results.json`) included Alice's full trace. Alice's `closure_criterion` evidence post-strip is qualitatively unchanged from M3-Eval close: the case still surfaces the UC-A↔UC-H mis-classification failure shape (`per_turn_trace[].tool_calls` + `phase_plan` + `projection` shape is independent of the stripped `outcome_checks` / `llm_judge_dimensions` lists, which only fed into the legacy programmatic dimensions that are no longer gates). Detailed `per_turn_trace[]` review is deliver-agent + human territory at sub-sprint close; the dev-side smoke produced the evidence file at `results/20260523-024300/results.json`.

## 7. Alice strip direction verification

The strip direction (Alice 7+3 → empty; not the reverse where the 11 sibling cases grow legacy fields) was verified against M3-Eval design + S-Eval-4 archive BEFORE the edit per `docs/sprint_objective.md` §7 stop condition.

**Evidence 1 — M3-Eval objective §schema convergence outcome** (`docs/milestones/M3-Eval_objective.md` line 114):

> Schema convergence outcome: CaseSpec demotes 6 hard surfaces (`expected_tool_sequence` / `forbidden_tools` / `escalation_trigger` exact-match / `bot_handling_pattern` / `outcome_checks` per-case identical checklist / `should_escalate` binary) from required/mandatory-effect to optional-with-default-off behaviour; a new optional `closure_criterion` string is introduced for bad-case and anchor_outcome suites.

The new optional `closure_criterion` field is the M3-Eval permanent design replacement for the legacy `outcome_checks` per-case checklist + the L3 `llm_judge_dimensions` triplet; the empty-list shape is the permanent post-S-Eval-1 schema for bad-case / anchor_outcome suites.

**Evidence 2 — S-Eval-1 scope #2** (`docs/milestones/M3-Eval_objective.md` line 127):

> Demote `outcome_checks.py` per-case identical 7-item checklist to advisory; introduce per-case opt-in via the existing `scoring.outcome_checks: list[str]` (preserved for backward compat; absent → no checks contribute to gate).

S-Eval-1 explicitly demotes the 7-item identical checklist to advisory + makes it opt-in via the `scoring.outcome_checks` field. Alice's pre-S2 7 items were the exact identical legacy checklist; stripping aligns Alice with the post-S-Eval-1 advisory-by-default schema.

**Evidence 3 — S-Eval-4 non-goal #5** (`docs/sprints/sprint-045-objective.md` line 234):

> 5. **Modify the existing Alice bad case** `alice_uc_a_uc_h_misclass.yaml` (cascade fence; re-run as regression guard).

S-Eval-4 explicitly fenced Alice out of the new-schema authoring round to preserve her regression-guard role. The 10-12 NEW bad cases authored in S-Eval-4 followed the new schema (empty `outcome_checks` + empty `llm_judge_dimensions` + populated `closure_criterion`); see `eval_interactive/case_specs/bad_cases/cs001_uc_c_mechanical_template_escalate.yaml` lines 86-87 for a sibling reference (empty lists). S2 is the unification cleanup that wasn't done at S-Eval-4 due to that explicit fence — the strip direction is the natural completion of the M3-Eval design-intent migration.

**S-Eval-5 (Sprint 46) L3 repositioning** (referenced via `docs/10-handoff.md` §1 lead M3-Eval close paragraph): the 3 legacy L3 dims (`groundedness` / `relevance` / `tone_appropriateness`) were demoted to Tier-3 advisory at M3-Eval close — these are exactly the 3 dims Alice carried in her `llm_judge_dimensions`. Stripping them to empty is consistent with their Tier-3 advisory status; their continued presence on Alice's spec would imply they're case-mandatory (which the M3-Eval pyramid rejects).

**Direction confirmed**: STRIP Alice's legacy fields to empty (default S2 direction per `docs/sprint_objective.md` §4 + `docs/milestone_objective.md` §3 S2 paragraph + human approval 2026-05-23). No §10 stop condition fired.

## 8. OQs surfaced

**OQ-S48.1** — The `bad_case_metadata` schema across the 12 bad cases is **not uniform**: cs001 (and other S-Eval-4 cases) use `source_session_id` inside `bad_case_metadata`, while Alice uses `original_session_id` (with `source_session_id` at top-level CaseSpec scope). cs001 carries a `tier: scope-relevant` field; Alice does NOT carry a `tier:` field at all. The dev-prompt's PRESERVE list named "source_session_id" inside `bad_case_metadata`, but Alice's field is named `original_session_id`; the equivalent semantic content is preserved (both are non-empty 36-char UUID-shape strings) but the field name differs. S2 did NOT rename / harmonise these field-name differences (out of scope — preserve-mandatory). **Surfaced for deliver-agent + human at close**: whether to open a future R-item (`R-bad-case-metadata-field-name-canonicalize`) to harmonise the `source_session_id` vs `original_session_id` field names across all 12 bad cases + add a missing `tier:` field to Alice. Low priority; documentation-only consistency.

**OQ-S48.2** — The `_resolve_case_passed_authority` helper currently surfaces `case_passed_authority` on the per-case result dict but does NOT propagate to `RunResult.summary` aggregate metrics. If a downstream consumer (HTML report, future Codex review aggregation) wants a top-level `suite_authority: human_review | programmatic | mixed` summary flag, that's a narrow follow-on extension. S2 did NOT add this (no consumer asked for it). **Surfaced for deliver-agent + human at close**: open `R-runresult-summary-authority-flag` (low priority, observability nicety) or close as not-needed.

**OQ-S48.3** — The `case_passed_authority` field is emitted on result dicts but the existing HTML report template (`eval_interactive/eval_interactive/report/html_report.py`) does NOT yet render it. The JSON results.json carries the field; the HTML view does not visually distinguish human_review from programmatic rows. S2 did NOT touch the HTML template (out of scope; report rendering is a downstream consumer concern). **Surfaced for deliver-agent + human at close**: open `R-html-report-human-review-annotation` (low priority, observability nicety) or defer to the next observability sprint.

## 9. Drift items

| Drift | Estimate (contract §4) | Actual | Note |
|-------|------------------------|--------|------|
| §4 row #5 — complexity LOW (1 YAML; mechanical edit) | LOW | LOW (2 / 12 net lines on alice YAML; mechanical strip of 2 lists) | Within target. |
| §4 row #2 — complexity LOW-MEDIUM (1-3 files; schema field + executor branching) | LOW-MEDIUM | MEDIUM (5 modified files + 1 NEW test file; 153 net insertions across modified files + 349 lines NEW test file) | Slightly above LOW-MEDIUM estimate. Drivers: (a) defensive null-handling for legacy mock-spec compat (`getattr(case_spec, "source_suite", None)` at 3 call sites); (b) emitting `case_passed_authority` on all 4 result-builder paths (`_build_case_result` + 3 placeholder builders) for schema uniformity; (c) 17 NEW tests across 4 logical groupings to cover the membership, resolution, parent-dir inference, and 4 builder paths. Above estimate but justified by S2 acceptance bar's NEW test requirement + by the need to keep `tests/trace/test_contract_validation.py` mock-spec tests green. |
| §4 row #5 + #2 mandatory PRESERVE on Alice metadata | UNCHANGED (binary) | UNCHANGED on all 6 named fields + all sibling fields | Within target. |

**Test-count target**: §6 acceptance bar "Python baseline preserved or improved — no NEW failures from S2; NEW tests for Item #2 should PASS". Achieved: net 0 new failures + 17 NEW passes (dev env: 7 → 7 failed; 424 → 441 passed). Within target.

## 10. Hard fence honored checklist

Per `docs/sprint_objective.md` §5:

- [x] `server/src/main/java/**` — UNTOUCHED. Java baseline `1163 / 1-inherited / 0 / 2` confirms.
- [x] `server/src/main/resources/skills/*.yaml` — UNTOUCHED.
- [x] `server/src/main/resources/system_prompt.txt` — UNTOUCHED.
- [x] `docs/runtime_freeze_and_risk_policy.md` — UNTOUCHED.
- [x] `docs/current/iteration_governance.md` — UNTOUCHED by dev (working-tree mod from deliver-agent close-bundle artefact NOT staged by dev per §11 bundle policy).
- [x] `docs/current/doc_governance.md` — UNTOUCHED by dev (working-tree mod from deliver-agent close-bundle artefact NOT staged by dev).
- [x] `docs/current/agent_context_guide.md` — UNTOUCHED.
- [x] `docs/sprints/sprint-NNN-*` — UNTOUCHED (only NEW `docs/sprints/sprint-048-handoff.md` created at S2 close, this very file).
- [x] `docs/milestones/*` — UNTOUCHED.
- [x] `docs/10-handoff.md` — UNTOUCHED by dev.
- [x] `docs/action_bank.md` — UNTOUCHED (R-item flip deferred to deliver-agent at close per §11 below).
- [x] `docs/codex-findings.md` — UNTOUCHED.
- [x] `docs/milestone_objective.md` — UNTOUCHED.
- [x] `eval_interactive/case_specs/bad_cases/cs*.yaml`, `fg5q*.yaml`, `iwzx*.yaml`, `wmkb*.yaml` — UNTOUCHED (only `alice_*.yaml` modified per §4 row #5; verified via `git diff --name-only eval_interactive/case_specs/bad_cases/`).
- [x] `eval_interactive/case_specs/bad_cases/_manifest.md` — UNTOUCHED.
- [x] `eval_interactive/case_specs/shadow/` — UNTOUCHED (no reads).
- [x] `eval_interactive/eval_interactive/scoring/composite.py` — UNTOUCHED (S3 territory; the `case_passed_authority` field rides on the result dict alongside the existing CompositeScore fields, not on `CompositeScore` itself, so the §5 EXCEPTION clause did NOT need to be exercised).
- [x] `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` — UNTOUCHED (S3 territory).
- [x] `eval_interactive/eval_interactive/batch/executor.py:365-392` `_compute_tier2_result` — UNTOUCHED (Tier-2 wiring is S3 territory; S2 only touched `run_batch`, `_execute_case_sync`, and the 4 result-builder methods + added the module-level `_resolve_case_passed_authority` helper).

Verification command: `git diff --name-only` returns the 5 S2-scope modified files plus the NEW test file plus this NEW handoff; all are within the §4 "Files in scope" set.

## 11. R-item flip request

Per `docs/sprint_objective.md` §6 (deliver-agent does the actual flips in `docs/action_bank.md` at close):

- `R-bad-case-fixture-migrate-to-l3-judge-dims` → **CLOSED** (Item #5 delivers Alice's strip to the M3-Eval empty-list schema; the migration intent recorded at M3-Eval close is now executed on the last remaining legacy fixture).

Proposed NEW R-items (deliver-agent + human jointly decide whether to open):

- (Optional, low priority) `R-bad-case-metadata-field-name-canonicalize` — harmonise `source_session_id` vs `original_session_id` field naming across the 12 bad cases + add missing `tier:` field to Alice — see §8 OQ-S48.1.
- (Optional, low priority) `R-runresult-summary-authority-flag` — add a top-level `suite_authority` summary flag on `RunResult.summary` — see §8 OQ-S48.2.
- (Optional, low priority) `R-html-report-human-review-annotation` — render `case_passed_authority` in the HTML report — see §8 OQ-S48.3.

## 12. Closure verdict (deferred to sub-sprint close)

To be appended at S-Cleanup-2 close by deliver-agent + human jointly. Reserved per existing convention.

Template (per `docs/sprint_objective.md` §11 row 12):

- **A — Clean PASS** / **B fix-iteration** / **C in-flight downgrade** / **out-of-scope-review**

Recommended (dev self-assessment, non-binding): **A — Clean PASS** based on (i) both items delivered with verification evidence; (ii) §4.1 self-walk clean approve per Q1-Q9; (iii) Java baseline unchanged `1163 / 1-inherited / 0 / 2`; (iv) Python net +17 passed / +0 failed in this dev env (projected +17 in deliver-agent env); (v) all 20 §5 hard fences honored; (vi) 17 NEW tests covering Item #2 authority logic all PASS; (vii) Alice strip direction independently verified against 3 M3-Eval / S-Eval-4 archive cells before the edit. The deliver-agent + human should consider OQ-S48.1 / OQ-S48.2 / OQ-S48.3 dispositions before finalising.
