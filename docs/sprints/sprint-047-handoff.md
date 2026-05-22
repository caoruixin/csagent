---
title: Sprint 47 / S-Cleanup-1 handoff — Eval harness polish + R-item closure
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff for Sprint 47 / S-Cleanup-1)
last_reviewed: 2026-05-23
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Dev handoff for Sprint 47 / S-Cleanup-1 (first sub-sprint of Milestone
  M4-Eval-Cleanup). §12 closure verdict reserved for deliver-agent +
  human at sub-sprint close.
---

# Sprint 47 / S-Cleanup-1 handoff — Eval harness polish + R-item closure

## 1. Identity

- **Sprint number**: 47 (global) / S-Cleanup-1 (M4-Eval-Cleanup sub-sprint 1; FIRST sub-sprint).
- **Milestone**: M4-Eval-Cleanup (`docs/milestone_objective.md`).
- **Branch**: `refactor/remove-the-shackles`.
- **HEAD prior to dev**: `4344662` (deliver-agent's M3-Eval close-bundle commit on top of S-Eval-5 dev PASS `7562a2d`).
- **Dev commit SHA**: appended at commit-time (see §11 R-item flip request + bundle policy §7).

## 2. Scope landed

Per `docs/sprint_objective.md` §4. Numstat is from `git diff --cached --numstat` (staged S1-scope files, pre-commit; same shape as `git show --numstat <commit>` post-commit).

| Item | File | insertions | deletions | Notes |
|------|------|------------|-----------|-------|
| #6 | `eval_interactive/case_specs/anchor/cs_interactive_001.yaml` | 1 | 0 | Added `- user_goal_achievement` to `llm_judge_dimensions`. outcome_class=escalate, primary_uc=UC-C. |
| #6 | `eval_interactive/case_specs/anchor/cs_interactive_004.yaml` | 1 | 0 | Added `- user_goal_achievement` to `llm_judge_dimensions`. outcome_class=resolve, primary_uc=UC-D. |
| #6 | `eval_interactive/case_specs/anchor/cs_interactive_012.yaml` | 1 | 0 | Added `- user_goal_achievement` to `llm_judge_dimensions`. outcome_class=resolve, primary_uc=UC-FP. |
| #1 | `eval_interactive/eval_interactive/batch/sets.py` | 20 | 8 | NEW `_OPT_IN_SETS = ("bad_cases", "anchor_outcome")` tuple + `_ALL_SETS = _KNOWN_SETS + _OPT_IN_SETS`. `load_set("all")` still iterates only `_KNOWN_SETS` (opt-in excluded by construction). Validation + `list_sets` use `_ALL_SETS`. Updated module docstring + `load_set` docstring. |
| #1 | `eval_interactive/eval_interactive/cli.py` | 6 | 1 | `--set` help text updated to list opt-in suites (bad_cases, anchor_outcome) separately + reference §5.6. |
| #3 | `eval_interactive/eval_interactive/config.py` | 4 | 2 | `BatchConfig.parallel` default `5 → 1` (line 85; 2-line rationale comment at lines 83-84); `_build_config` loader fallback `5 → 1` (line 138) for consistency. |
| #8 | `eval_interactive/tests/scoring/test_escalation_enum_sync.py` | 35 | 27 | Migrated from deleted `docs/customer_service_tool_spec_v0_2.yaml` (yaml parse) to `docs/current/customer_service_tool_spec_v0_3.md` (markdown regex parse of the canonical-enum bullet). Renamed `_load_yaml_enum` → `_load_spec_enum`; renamed yaml-prefixed tests to spec-prefixed; removed `import yaml` (unused). |
| #3 | `eval_interactive/tests/test_config.py` | 1 | 1 | `test_defaults_for_missing_sections` assertion `parallel == 5` → `parallel == 1` to track Item #3 default change. |

**Total**: 8 files changed, +69 / -39 (`git diff --cached --shortstat`).

**Files NOT staged** (deliver-agent / human bundle at launch commit per `iteration_governance.md` §8.7):

- `docs/milestone_objective.md` (deliver-agent)
- `docs/sprint_objective.md` (deliver-agent)
- `compact/sprint-047-dev-prompt.md` (deliver-agent — untracked)
- `compact/sprint-deliver-orchestrator.md` (deliver-agent template update unrelated to S1)
- `compact/deliver-activation.md` (deliver-agent — untracked)
- `docs/10-handoff.md` (deliver-agent)
- `docs/current/doc_governance.md` (deliver-agent)
- `docs/current/iteration_governance.md` (deliver-agent)
- `docs/current/deliver_close_taxonomy.md` (deliver-agent — untracked)

## 3. §3 layer-classification + anti-hardcode self-walk

The §7 stanza in `docs/sprint_objective.md` §3 holds against the actual commit:

- **Target failure layer**: `infra` (primary — CLI registration #1, default value #3, stale test migration #8, test assertion follow-on for #3) + `eval_spec` (secondary — fixture seeding #6).
- **Tier-0 invariant**: This sprint adds no Tier-0 invariant. `docs/runtime_freeze_and_risk_policy.md` §1 / §2 untouched (confirmed below in hard-fence checklist).
- **Semantic hardcode**: No semantic hardcode introduced. (a) `_OPT_IN_SETS` is a configuration tuple naming subdirectories, not a semantic decision. (b) `parallel: int = 1` is an infra parameter. (c) `user_goal_achievement` fixture seeding is data on an existing recognised advisory dim (`LlmJudge.ALL_DIMENSIONS`, `_ADVISORY_DIMENSIONS`); no schema change. (d) Test path migration #8 is a regex over markdown enum tokens; not a semantic rule. (e) Test assertion #3 follow-on tracks the configuration change.
- **Generalization coverage**: Not applicable — this sub-sprint adds no new semantic surface.

### §4.1 nine-question kernel self-walk

Per `iteration_governance.md` §4.1, walked against the S1 staged diff:

1. **Q1 (keyword/regex/if-else/enum/per-UC matrix for semantic decision)** — NO. `_OPT_IN_SETS` is a directory-name tuple (infra configuration); not a decision rule. The test_escalation_enum_sync regex parses canonical enum tokens from the v0_3 markdown; no new semantic content introduced (the regex consumes the existing spec).
2. **Q2 (justified as protecting Tier-0)** — N/A (no Q1 yes).
3. **Q3 (could be projected as soft signal instead)** — N/A (Q1 = no).
4. **Q4 (encodes visible-eval case text / trace phrasing / CaseSpec id)** — NO. The seeded `user_goal_achievement` dim name is a generic L3 advisory dim, not case-specific text. `_OPT_IN_SETS` references directory names, not case ids.
5. **Q5 (moves semantic ownership LLM → Java)** — NO. The LLM-side semantic surface is untouched. The judge for `user_goal_achievement` (in `LlmJudge._judge_user_goal_achievement`) already exists and was wired in S-Eval-5; this sub-sprint only opts 3 fixtures into the existing advisory dim.
6. **Q6 (adds if-else block to prompt)** — NO. `server/src/main/resources/system_prompt.txt` UNTOUCHED.
7. **Q7 (preserves tool schema / capability / PII / grounding floor)** — YES (preserved). Tool schema, capability boundary, PII floor, grounding floor are all server-side; S1 makes no `server/` changes.
8. **Q8 (ships target/neighbor/negative/shadow coverage)** — N/A (no new semantic surface; §3 stanza's generalization-coverage stanza says N/A).
9. **Q9 (rollback plan if temporary)** — N/A (changes are durable infra cleanups, not temporary measures); `git revert <s1-commit>` is the rollback path if needed.

**Verdict (self-assessment)**: `approve`. Clean §4.1 pass per Q1-Q9 because S1 is infra + char-test + R-item closure (no semantic hardcode introduced); no question yielded a non-trivial "yes" or "concern".

## 4. Java baseline

`cd server && mvn test -q` (no flags):

```
[ERROR] Failures:
[ERROR]   SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53
    ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor ==> expected: <true> but was: <false>
[ERROR] Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2
```

**Baseline preservation**: `1163 / 1-inherited / 0 / 2` — IDENTICAL to M3-Eval close baseline at HEAD `7562a2d` per `docs/10-handoff.md` §0. The 1 inherited failure (Sprint 24-era working-tree mod; "Sprint 6" anchor absent since then) persists per OQ-S41.5 / S-Eval-4 STATUS QUO.

Reproducible command: `cd server && mvn test -q 2>&1 | tail -3`.

## 5. Python baseline

`cd eval_interactive && uv run pytest --tb=no -q` (full suite):

```
7 failed, 424 passed in 11.15s
```

Pre-change baseline (in this dev environment, same `uv run pytest` invocation, S1 changes stashed):

```
9 failed, 422 passed in 11.15s
```

**Net delta from S1**: −2 failed, +2 passed.

**Specific test flips driven by S1**:

| Test | Pre-S1 | Post-S1 | Driver |
|------|--------|---------|--------|
| `tests/scoring/test_escalation_enum_sync.py::test_yaml_and_eval_schema_enums_match` (RENAMED `test_spec_and_eval_schema_enums_match`) | FAIL (deleted v0_2.yaml) | PASS (v0_3 markdown parse) | Item #8 |
| `tests/scoring/test_escalation_enum_sync.py::test_yaml_and_runtime_enums_match` (RENAMED `test_spec_and_runtime_enums_match`) | FAIL (deleted v0_2.yaml) | PASS (v0_3 markdown parse) | Item #8 |
| `tests/scoring/test_escalation_enum_sync.py::test_runtime_and_eval_schema_enums_match` | PASS (unchanged; reads only Java runtime) | PASS | Item #8 (no behaviour change for this assertion) |
| `tests/test_config.py::TestLoadConfig::test_defaults_for_missing_sections` | PASS (asserts `parallel == 5`) | PASS (asserts `parallel == 1` post-S1 update) | Item #3 follow-on |

**Remaining 7 failures (all baseline pre-existing; NOT introduced by S1)**:

1. `tests/regression/test_case_spec_overrides.py::test_v2_schema_loads_cleanly`
2. `tests/regression/test_case_spec_overrides.py::test_smoke_review_report_tracks_smoke_set_and_overrides`
3. `tests/regression/test_corpus_lint.py::test_regenerated_corpus_bucket_lints_clean[anchor]`
4. `tests/regression/test_corpus_lint.py::test_regenerated_corpus_bucket_lints_clean[promotion]`
5. `tests/regression/test_corpus_lint.py::test_regenerated_corpus_bucket_lints_clean[exploration]`
6. `tests/regression/test_corpus_lint.py::test_regenerated_corpus_bucket_lints_clean[smoke]`
7. `tests/regression/test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag`

The 5 `test_corpus_lint.py` failures all stem from `ModuleNotFoundError: No module named 'eval_interactive.case_spec'` when a subprocess (`sys.executable -m eval_interactive.case_spec.linter ...`) is invoked from outside the uv-managed venv (the dev environment's `sys.executable` resolves to a miniconda Python that lacks the package). This is an environment-dependent baseline; it is observed both pre- and post-S1, identical failure shape, identical line. The deliver-agent's documented baseline (`5 failed / 426 passed` at HEAD `7562a2d` per `docs/10-handoff.md` §0) likely did not surface these failures because the deliver-agent's environment runs the subprocess with the uv venv Python (where the package IS importable). The contract acceptance bar "baseline 5 fail → 4 fail (or fewer)" applies to the deliver-agent's environment count.

**Baseline-environment reconciliation (deliver-agent's reference count)**: in the deliver-agent's environment with baseline `5 failed / 426 passed`, the S1 changes flip 2 enum_sync tests pass and introduce no new failures (Item #3 follow-on test_config update keeps that test passing). Projected post-S1 deliver-environment count: **3 failed / 428 passed** (5 − 2 = 3). Meets the §6 acceptance bar of "5 → 4 or fewer".

Reproducible command: `cd eval_interactive && uv run pytest --tb=no -q 2>&1 | tail -1`.

## 6. Per-item verification evidence

### Item #1 (sets.py + CLI register bad_cases + anchor_outcome)

Implementation: separate `_OPT_IN_SETS` tuple alongside `_KNOWN_SETS` (chosen for clarity — preserves `--set all` semantics by construction without per-entry branching).

```
$ uv run python -c "from eval_interactive.batch.sets import _KNOWN_SETS, _OPT_IN_SETS, _ALL_SETS; \
                     print('_KNOWN_SETS:', _KNOWN_SETS); print('_OPT_IN_SETS:', _OPT_IN_SETS); print('_ALL_SETS:', _ALL_SETS)"
_KNOWN_SETS: ('anchor', 'promotion', 'exploration', 'smoke')
_OPT_IN_SETS: ('bad_cases', 'anchor_outcome')
_ALL_SETS: ('anchor', 'promotion', 'exploration', 'smoke', 'bad_cases', 'anchor_outcome')
```

End-to-end resolve verification:

```
$ uv run python -c "from eval_interactive.batch.sets import CaseSetManager; \
                     m=CaseSetManager(base_dir='case_specs'); \
                     print('list_sets:', m.list_sets()); \
                     print('bad_cases loaded:', len(m.load_set('bad_cases'))); \
                     print('anchor_outcome loaded:', len(m.load_set('anchor_outcome'))); \
                     print('all loaded:', len(m.load_set('all')))"
list_sets: {'anchor': 159, 'promotion': 101, 'exploration': 107, 'smoke': 14, 'bad_cases': 12, 'anchor_outcome': 12}
bad_cases loaded: 12
anchor_outcome loaded: 12
all loaded: 381
```

`all` count: 159 + 101 + 107 + 14 = 381 (opt-in suites excluded). CLI help text updated at `cli.py:140-147` to flag opt-in nature explicitly.

### Item #3 (default parallel 5 → 1)

```
$ grep -n "parallel" eval_interactive/eval_interactive/config.py
83:    # parallel=1 default per M3-Eval close evidence (bad-case suite stability
84:    # requires sequential runs); opt-in to higher parallel via CLI --parallel.
85:    parallel: int = 1
138:        parallel=int(batch_raw.get("parallel", 1)),
```

Both the dataclass default (line 85) AND the loader fallback (line 138) align at `1`. Live config behaviour (with the repo's `eval_interactive/eval_interactive.yaml`):

```
$ uv run python -c "from eval_interactive.config import load_config, BatchConfig; \
                     print('dataclass default:', BatchConfig().parallel); \
                     print('loaded config batch.parallel:', load_config().batch.parallel)"
dataclass default: 1
loaded config batch.parallel: 3
```

**Runtime delta investigation (per contract §investigation requirement)**: The dataclass default at `config.py:85` is **shadowed at runtime** by `eval_interactive/eval_interactive.yaml:29` which carries `batch.parallel: 3` (unchanged by S1). The change therefore takes effect ONLY in:

- Unit-test contexts that construct `Config()` / `BatchConfig()` without a yaml (e.g., `tests/test_config.py::test_defaults_for_missing_sections`, now updated to assert `== 1`).
- Setups missing `batch.parallel` from the yaml (uncommon).

In typical CLI usage (`eval-interactive run --set ...` against the repo's yaml), the effective `parallel` is still `3` (or whatever `--parallel` overrides). Therefore the "anchor + smoke runtime ≥ 3x slower" stop-condition is **N/A** by construction — the global default change does not propagate through the yaml path. No per-suite default override fallback is required; ship the global default change as written.

This finding may be useful to deliver-agent: if the intent of "parallel=1 default for bad-case stability" needs to be enforced at the YAML level too, that's a separate edit (`eval_interactive/eval_interactive.yaml:29`) outside S1 scope. See §8 OQs.

### Item #6 (seed user_goal_achievement on 2-3 anchor fixtures)

Selection criteria: diverse outcome classes + UCs (only `escalate` + `resolve` exist in the anchor corpus; no `decline` / `defer` cases — distribution: 82 escalate + 77 resolve).

```
$ uv run python -c "from eval_interactive.case_spec.loader import load_case_spec; from pathlib import Path; \
                     [print(f'{fn}: outcome_class={load_case_spec(Path(\"case_specs/anchor\")/fn).expected.outcome_class}, primary_uc={load_case_spec(Path(\"case_specs/anchor\")/fn).expected.primary_uc}, dims={load_case_spec(Path(\"case_specs/anchor\")/fn).scoring.llm_judge_dimensions}') for fn in ['cs_interactive_001.yaml','cs_interactive_004.yaml','cs_interactive_012.yaml']]"
cs_interactive_001.yaml: outcome_class=escalate, primary_uc=UC-C, dims=['groundedness', 'relevance', 'tone_appropriateness', 'user_goal_achievement']
cs_interactive_004.yaml: outcome_class=resolve, primary_uc=UC-D, dims=['groundedness', 'relevance', 'tone_appropriateness', 'user_goal_achievement']
cs_interactive_012.yaml: outcome_class=resolve, primary_uc=UC-FP, dims=['groundedness', 'relevance', 'tone_appropriateness', 'user_goal_achievement']
```

Coverage: 1 escalate + 2 resolve; 3 distinct UCs (UC-C, UC-D, UC-FP). All 3 fixtures still load end-to-end via `load_case_spec` (no schema validation error). Grep count:

```
$ grep -rl "user_goal_achievement" eval_interactive/case_specs/anchor/ | wc -l
       3
```

Acceptance bar (`≥ 2 matches`) met.

### Item #7 (verify cs_interactive_095 references)

**Audit claim**: "cs_interactive_095 was deleted but still referenced."

**Deliver-agent dispute**: "the file still exists at `eval_interactive/case_specs/anchor/cs_interactive_095.yaml`."

**Investigation result**: deliver-agent dispute **confirmed**. The file exists at `anchor/cs_interactive_095.yaml` (2.1K, last-modified by deliver-agent's M3-Eval close-bundle commit `4344662`).

Repo-wide grep for `cs_interactive_095`:

```
$ grep -rn "cs_interactive_095" /Users/caoruixin/projects/csagent-latest \
    --include="*.yaml" --include="*.md" --include="*.py" \
    --exclude-dir=".git" --exclude-dir="node_modules"
```

Returns 41 matches. Classification:

| Category | Count | Status |
|----------|-------|--------|
| Direct file path references (paths that must exist) | 1 | **1 orphan found** — see below |
| `case_id` / text mentions in active config + code (file IS named, but the reference is textual, not a path) | 6 | Not orphan; file exists |
| Sprint archive references (`docs/sprints/sprint-NNN-*`, `docs/archive/*`) | 32 | Historical; not orphan |
| Current sprint dev-prompt (`compact/sprint-047-dev-prompt.md`) | 4 | This sprint's own brief; not orphan |
| Bad-case derived case_id (`bad_cases/cs095_*`) + its manifest entry | 2 | Distinct file `cs095_uc_d_email_recovery_misroute.yaml`; not orphan |

**ONE genuine orphan reference identified**: `eval_interactive/case_specs/case_families/_manifest.yaml:114` declares:

```yaml
target_case_path: eval_interactive/case_specs/smoke/cs_interactive_095.yaml
```

That file does NOT exist at the `smoke/` path (the actual file is at `anchor/cs_interactive_095.yaml`). All 8 other `target_case_path` references in the same manifest correctly resolve to existing `smoke/*.yaml` files; only the cs095 entry is broken. The case_families manifest is **documentation only** — not consumed by any code path (verified via `grep -rn "case_families/_manifest" --include="*.py"` returning 0 matches).

**Decision**: NOT fixed in S1 (per conservative read of #7 hard fence "do NOT delete `cs_interactive_095.yaml` itself"; while a 1-line manifest path edit is technically trivial, the Sprint-20-era case_families organisational logic — why is cs095 alone not in smoke when 8 sibling target paths are — is broader than S1's mandate). Surfaced as proposed deferred R-item — see §8 OQs.

### Item #8 (test_escalation_enum_sync v0_2 → v0_3 migration)

v0_3 spec file location: `docs/current/customer_service_tool_spec_v0_3.md`. The canonical 23-value enum is embedded as a markdown bullet list inside the `### \`request_handover\`` section (lines 133-144). The bullet starts with `**Canonical \`escalation_reason\` enum (23 values)**:` and ends with `. The same set is mirrored ...`; values are backtick-quoted lowercase_underscore tokens separated by commas.

Migration approach: regex extract the body between the heading marker and the closing sentence; collect all backtick-quoted lowercase tokens. No yaml structure to parse; no `import yaml` needed.

```
$ uv run pytest tests/scoring/test_escalation_enum_sync.py -v
tests/scoring/test_escalation_enum_sync.py::test_spec_and_eval_schema_enums_match PASSED [ 33%]
tests/scoring/test_escalation_enum_sync.py::test_runtime_and_eval_schema_enums_match PASSED [ 66%]
tests/scoring/test_escalation_enum_sync.py::test_spec_and_runtime_enums_match PASSED [100%]
============================== 3 passed in 0.02s ===============================
```

All 3 tests PASS. Sub-helper rename: `_load_yaml_enum` → `_load_spec_enum`; constant rename: `TOOL_SPEC_YAML` → `TOOL_SPEC_MD`; test function renames: `test_yaml_and_*` → `test_spec_and_*` for accuracy.

### Item #10 (cleanup --output references in compact dev-prompt template)

```
$ grep -n "eval-interactive run.*--output\|--output" compact/sprint-deliver-orchestrator.md
0 matches for 'eval-interactive run.*--output\|--output'
```

**No matches.** `compact/sprint-deliver-orchestrator.md` does not contain a `--output` reference. Per the contract §10 stop-condition: #10 is a **no-op**.

The audit's `--output` finding was in `compact/sprint-046-dev-prompt.md` (a sprint archive — immutable per `doc_governance.md`); the active template is clean. No edit needed.

## 7. #7 investigation (detail)

See §6 Item #7 above. Summary:

- Audit claim: cs_interactive_095 deleted but referenced — **DISPUTED** (file exists at `anchor/cs_interactive_095.yaml`).
- One real but docs-only orphan reference found: `case_families/_manifest.yaml:114` points to non-existent `smoke/cs_interactive_095.yaml` (file is at `anchor/`).
- Manifest is documentation; not consumed by code (verified via grep).
- Conservative S1 disposition: surface as proposed deferred R-item rather than 1-line manifest fix; deliver-agent + human decide at close.

## 8. OQs surfaced

**OQ-S47.1** — Should the YAML-resident `batch.parallel: 3` at `eval_interactive/eval_interactive.yaml:29` ALSO be aligned to `1` to match the new dataclass default and the M3-Eval close evidence? S1 changed only `config.py` (per contract §4 row #3). With the yaml at 3, the global default change is shadowed at runtime in typical CLI usage. **Surfaced for deliver-agent + human at close**: keep yaml at `3` (current S1 disposition), align yaml to `1` (full intent execution), or escalate as separate R-item.

**OQ-S47.2** — Should the orphan `case_families/_manifest.yaml:114` `target_case_path` (pointing to non-existent `smoke/cs_interactive_095.yaml`; file is at `anchor/`) be fixed in S1 as a trivial 1-line manifest edit, or deferred? S1 disposition: **deferred** (conservative read of #7 hard fence). **Surfaced for deliver-agent + human at close**: confirm deferral and open `R-case-families-manifest-cs095-smoke-vs-anchor-orphan` (or similar), or approve a 1-line manifest fix as in-scope cleanup.

**OQ-S47.3** — The pre-S1 baseline in this dev environment (`9 failed / 422 passed`) differs from the deliver-agent's documented baseline (`5 failed / 426 passed` per `docs/10-handoff.md` §0) by 4 environment-specific `test_corpus_lint.py` failures (`ModuleNotFoundError` from a subprocess running miniconda Python instead of the uv-managed venv Python). Net S1 delta in this env: −2 failed / +2 passed (Item #8 fixes 2 enum_sync tests; no new failures). **Surfaced for deliver-agent**: do these 4 `test_corpus_lint.py` failures also surface in the deliver-agent's environment, or are they uniquely a dev-env artifact? If they surface, projected post-S1 deliver-environment count is `3 failed / 428 passed` (5 − 2 = 3); if not, it remains `3 failed / 428 passed` either way (no S1-introduced failures regardless).

## 9. Drift items

| Drift | Estimate (contract §4) | Actual | Note |
|-------|------------------------|--------|------|
| §4 row #1 — complexity LOW | LOW | LOW (28 net lines across sets.py + cli.py) | Within target. |
| §4 row #3 — complexity LOW | LOW | LOW (6 net lines on config.py; 2 net lines on test_config.py) | Within target. Plus runtime-fallback investigation N/A (yaml shadow). |
| §4 row #6 — complexity LOW-MEDIUM | LOW-MEDIUM | LOW (3 net lines across 3 anchor fixtures) | Below estimate; trivial since `user_goal_achievement` is already a recognised advisory dim. |
| §4 row #7 — complexity LOW | LOW | LOW (grep + classification; no file edit) | Within target. |
| §4 row #8 — complexity LOW-MEDIUM | LOW-MEDIUM | MEDIUM-low (62-line net diff on the test file due to rename + docstring updates + import yaml removal; +35 / -27) | Slightly above estimate; rename for clarity (`_load_yaml_enum` → `_load_spec_enum`; `test_yaml_and_*` → `test_spec_and_*`) is the marginal cost. Reasoning documented in §6 Item #8. |
| §4 row #10 — complexity LOW | LOW | NO-OP (0 net lines) | Below estimate (template was already clean). |

**Test-count target**: contract §6 acceptance bar "Baseline 5 fail → 4 fail (or fewer)". Achieved net −2 in this dev environment (9 → 7); projected `5 → 3` in deliver-agent's environment. Within target.

## 10. Hard fence honored checklist

Per `docs/sprint_objective.md` §5:

- [x] `server/src/main/java/**` — UNTOUCHED. Java tests `1163 / 1-inherited / 0 / 2` confirms.
- [x] `server/src/main/resources/skills/*.yaml` — UNTOUCHED.
- [x] `server/src/main/resources/system_prompt.txt` — UNTOUCHED.
- [x] `docs/runtime_freeze_and_risk_policy.md` — UNTOUCHED.
- [x] `docs/current/iteration_governance.md` — UNTOUCHED.
- [x] `docs/current/doc_governance.md` — UNTOUCHED.
- [x] `docs/current/agent_context_guide.md` — UNTOUCHED.
- [x] `docs/sprints/sprint-NNN-*` — UNTOUCHED (only NEW `docs/sprints/sprint-047-handoff.md` created at S1 close, this very file).
- [x] `docs/milestones/*` — UNTOUCHED.
- [x] `docs/10-handoff.md` — UNTOUCHED.
- [x] `docs/action_bank.md` — UNTOUCHED (R-item flips deferred to deliver-agent at close per §11 below).
- [x] `docs/codex-findings.md` — UNTOUCHED.
- [x] `docs/milestone_objective.md` — UNTOUCHED.
- [x] `eval_interactive/case_specs/bad_cases/*` — UNTOUCHED (only READ for #7 verification).
- [x] `eval_interactive/case_specs/shadow/` — UNTOUCHED (no reads).
- [x] `compact/sprint-NNN-dev-prompt.md` archives — UNTOUCHED.
- [x] `eval_interactive/eval_interactive/scoring/composite.py` — UNTOUCHED (S3 territory).
- [x] `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` — UNTOUCHED (S3 territory).
- [x] `eval_interactive/eval_interactive/batch/executor.py` `_compute_tier2_result` lines 365-392 — UNTOUCHED.

Verification command: `git diff --cached --name-only` returns the 8 S1-scope files plus this new handoff; all are within the §4 "Files in scope" set.

## 11. R-item flip request

Per `docs/sprint_objective.md` §6 (deliver-agent does the actual flips in `docs/action_bank.md` at close):

- `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` → **CLOSED** (Item #8 delivers the migration; the 2 baseline-failing tests now PASS against the v0_3 markdown spec; the test no longer depends on the deleted v0_2 yaml).
- `R-bad-case-parallel-session-establishment-flakiness` → **PARTIAL-CLOSED** (Item #3 reduces the global `parallel` default to 1, aligning with the M3-Eval close evidence used for bad-case rerun stability; full closure requires deeper Tier-2 wiring or session-establishment redesign per the R-item's original scope).

Proposed NEW R-items (deliver-agent + human jointly decide whether to open):

- `R-case-families-manifest-cs095-smoke-vs-anchor-orphan` (1-line orphan in `eval_interactive/case_specs/case_families/_manifest.yaml:114`; not code-consumed; trivial 1-line fix or close-as-known-stale) — see §8 OQ-S47.2.
- (Optional, deliver-agent's call) `R-eval-yaml-parallel-default-alignment` — align `eval_interactive/eval_interactive.yaml:29` `batch.parallel: 3 → 1` to match dataclass default and M3-Eval close evidence; covers the §6 Item #3 investigation finding — see §8 OQ-S47.1.

## 12. Closure verdict (deferred to sub-sprint close)

To be appended at S-Cleanup-1 close by deliver-agent + human jointly. Reserved per existing convention.

Template (per `docs/sprint_objective.md` §11 row 12):

- **A — Clean PASS** / **B fix-iteration** / **C in-flight downgrade** / **out-of-scope-review**

Recommended (dev self-assessment, non-binding): **A — Clean PASS** based on (i) all 6 items delivered or no-op'd with verification evidence; (ii) §4.1 self-walk clean approve per Q1-Q9; (iii) Java baseline unchanged; (iv) Python net −2 failed in this dev env (projected −2 in deliver-agent env); (v) all 19 §5 hard fences honored. The deliver-agent + human should consider OQ-S47.1 and OQ-S47.2 dispositions before finalising.
