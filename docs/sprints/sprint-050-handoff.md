---
title: Sprint 50 / M5 S1 handoff — Eval Report Coherence (four-tier verdict surface + phase5 §6.9 fold-back)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff for Sprint 50 / M5 S1)
last_reviewed: 2026-05-24
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Dev handoff for Sprint 50 / M5 S1 (first sub-sprint of Milestone M5 —
  Observability Coherence). §12 closure verdict reserved for
  deliver-agent + human at sub-sprint close. S1 is §7-exempt (display
  + docs-only); the §4.1 self-walk is in §3 below for Codex clarity.
  No `server/` touch; zero scoring change; zero fixture change.
---

# Sprint 50 / M5 S1 handoff — Eval Report Coherence

## 1. Identity

- **Sprint number**: 50 (global) / M5 S1 (Milestone M5 — Observability Coherence sub-sprint 1; first of an expected 3-4 sub-sprints per `docs/milestone_objective.md` §3).
- **Milestone**: M5 — Observability Coherence (`docs/milestone_objective.md`). Path 1 research-driven scoping.
- **Branch**: `refactor/remove-the-shackles`.
- **HEAD prior to dev**: `84ae017` (M4-Eval-Cleanup milestone close commit — `deliver-agent: M4-Eval-Cleanup milestone close (A — Clean PASS)`).
- **Dev commit SHA**: appended at commit time (see §11 bundle policy).
- **Mode**: §7-exempt (display + docs-only); `infra` (eval-harness report rendering) + a single intentional **foundational docs fold-back** (`docs/foundational/phase5_evaluation_design.md §6.9`).
- **Human-confirmed scope choice (2026-05-24)**: **Alternative B** — remove the Phase-5 §6.9 7-metric dashboard *rendering* (HTML only; aggregates stay in `results.json`) + a foundational fold-back marking §6.9 superseded. The research proposal at `docs/solutions/eval_report_coherence_proposal.md` had recommended Alternative A (keep the legacy view, re-label it); the human chose B for the cleaner end-state.

## 2. Scope landed

Per `docs/sprint_objective.md` §"Scope (numbered; this is the contract)". Numstat is from `git diff --numstat <S1-files>` against HEAD `84ae017` (same shape as `git show --numstat <commit>` post-commit).

| Item | File | insertions | deletions | Notes |
|------|------|------------|-----------|-------|
| #1 + #2 | `eval_interactive/eval_interactive/report/html_report.py` | 575 | 180 | Full rewrite of the renderer to the M3-Eval four-tier verdict surface. (a) NEW `_resolve_suite_authority(summary, case_results)` static method — prefers the aggregate flag from `_compute_summary`, falls back to per-case derivation when re-rendering historical `results.json` files without the field. (b) NEW `_tier0_outcome`, `_tier1_label`, `_tier2_summary` static helpers — single source of truth for the per-case tier-labelled badge values. (c) NEW `_header_section` builds the `suite_authority` badge + the "programmatic counts are informational" guidance line. (d) NEW `_tier0_section` (L1 hard-checks aggregate by `check_name`, labelled Tier-0 Safety Floor). (e) NEW `_tier1_section` (Tier-1 Outcome — PRIMARY for human-judgment suites; surfaces `suite_authority`, the informational programmatic `case_passed` count, the authority split, and the manifest pointer). (f) NEW `_tier2_section` (Critical-flow aggregate — PASS / FAIL-critical / FAIL-advisory / N/A counts + per-case `failed_step_ids` tables). (g) NEW `_tier3_section` (Polish — advisory L2/L3 aggregate clearly marked "never flips case_passed"). (h) **NEW `_render_tier2(tier2_result)`** — per-case rendering of `passed` / `severity` / `failed_step_ids` / per-step outcomes (the field has been populated on every case dict by the executor since S-Eval-2 but was **never rendered**). (i) `_render_case` rewritten — per-case header now shows tier-labelled badges (`T0 PASS · T1 HUMAN_REVIEW · T2 PASS`) instead of the single binary PASS/FAIL `<span class="badge">`. **REMOVED**: `_metrics_dashboard` (the Phase-5 §6.9 7-card dashboard render) + `_pass_fail_overview` (the "0/N Passed" headline framing). `_per_uc_table` is **KEPT but REFRAMED** as "Per Use-Case Breakdown (programmatic-suite rollup)" with an informational note when `suite_authority` is `human_review`. `_render_l1` / `_render_l2` / `_render_l3` / `_regression_section` / `_metric_color` retained / lightly relabelled; per-case L2/L3 drawer headers renamed from "L2 Outcome Scores" / "L3 LLM Judge Scores" to "Tier-3 — L2 Advisory Outcome Scores" / "Tier-3 — L3 Advisory Judge Scores" to align with the post-M3 advisory framing. |
| #3 | `eval_interactive/eval_interactive/batch/executor.py` | 40 | 1 | Added a single field `"suite_authority"` to the dict returned by `_compute_summary`. Resolved **once per run** from per-case `case_passed_authority` annotations (which `_resolve_case_passed_authority` already populates per case from `CaseSpec.source_suite` via `is_human_judgment_suite`): `{"human_review"}` → `"human_review"`, `{"programmatic"}` → `"programmatic"`, otherwise → `"mixed"`. Empty case list → `"programmatic"` (matches the safety default the per-case resolver uses). **No existing `_compute_summary` field is modified** — only an additive ADD per the hard fence in `sprint_objective.md`. The legacy aggregates (`passed_cases` / `failed_cases` / `task_success_rate` / `stall_rate` / `mean_composite_score` / `mean_outcome_score` / `mean_judge_score` / `per_uc_breakdown` / `escalation_correctness` / `policy_compliance_rate` / `mean_turns_to_resolution`) STAY computed and STAY in `results.json` — only the HTML rendering of the §6.9 dashboard is removed. Updated docstring records the Sprint 50 / M5 S1 origin + the single source of truth for `suite_authority`. |
| #3 | `eval_interactive/eval_interactive/report/json_report.py` | 23 | 4 | Mirror update so the JSON renderer surfaces `suite_authority` (and now per-case `case_passed_authority` + `tier2_result`) alongside the HTML renderer — single source of truth, no drift (proposal §11). `_build_summary` reads `summary.get("suite_authority", "programmatic")`; `_build_case` adds `"case_passed_authority"` (defaulting to `"programmatic"`) and `"tier2_result"`. Module docstring refreshed to record the post-M3 framing + the §6.9-aggregates-stay-but-display-removed shape. |
| #4 | `docs/foundational/phase5_evaluation_design.md` | 34 | 0 | **Foundational fold-back** (the one intentional foundational doc edit this sub-sprint). (a) Added YAML front matter — `doc_tier: foundational`, `status: partial`, `implementation_status: partial`, `last_reviewed: 2026-05-24`, `source_of_truth` pointer that explicitly names §6.9 supersession, and a `notes:` block explaining the fold-back. (b) Added a clearly-marked supersession block immediately **before** the §6.9 section header pointing to `docs/milestones/M3-Eval_objective.md` (the four-tier pyramid) + `docs/current/iteration_governance.md` §5.5 (smoke composite_score demoted) + §5.6 (curated bad-case suite as primary gate). **§6.9 content is PRESERVED** — the 7-metric table and definitions remain in place per `doc_governance.md`'s "do not silently rewrite a foundational spec" rule. The supersession block also documents that the three N/A metrics (`correct_tool_invocation_rate` / `escalation_correctness_rate` / `grounded_final_answer_rate`) remain out of scope for this sub-sprint per `sprint_objective.md` §"Hard fences". No other phase5 section is rewritten. |
| #6 | `eval_interactive/tests/test_report_generators.py` | 450 | 3 | Extended the existing 15-test suite with **26 new tests** across **5 new test classes**. (a) `TestFourTierVerdictSurface` (7 tests) — Tier-0/1/2/3 section headers present; `suite_authority` badges (human-review + programmatic); per-case tier-labelled badges; informational-note rendering; Tier-2 failure case_ids + `failed_step_ids` surface; per-UC table kept + reframed. (b) `TestSuiteAuthorityResolution` (5 tests) — all-human_review / all-programmatic / mixed / summary-field-wins / empty-fallback. (c) `TestRenderTier2` (3 tests) — PASS surfaces PASS badge + per-step table; FAIL surfaces `failed_step_ids` + critical severity; None payload returns the "No Tier-2 result" placeholder. (d) `TestPhase5DashboardRemoved` (2 tests) — regression that "Key Metrics Dashboard" / "Pass / Fail Overview" + the Phase-5 card labels are GONE + that the JSON aggregates STAY (backward-compat). (e) `TestJsonReportSuiteAuthority` (3 tests) — JSON emits `suite_authority` for human_review + programmatic + per-case authority/tier2_result. (f) `TestComputeSummarySuiteAuthority` (6 tests) — executor `_compute_summary` returns the right `suite_authority` value across all_human_review / all_programmatic / mixed / empty / unannotated-coalesces-to-programmatic + that existing aggregates are preserved. **2 existing tests** lightly updated for the new badge shape: `test_html_contains_pass_fail_badges` now asserts the tier-labelled badge format (`>PASS<` / `>FAIL<`) since the binary `class="badge pass"`/`class="badge fail"` single-span was replaced by tier-labelled badges. **15 existing tests** all still pass unchanged. Module docstring updated to record the Sprint 50 / M5 S1 extension. |

**Total S1-scope mutations** (modified files only, via `git diff --shortstat <S1-files>`):

```
5 files changed, 1122 insertions(+), 188 deletions(-)
```

Reproducible commands:

```bash
git diff --numstat -- \
  eval_interactive/eval_interactive/report/html_report.py \
  eval_interactive/eval_interactive/report/json_report.py \
  eval_interactive/eval_interactive/batch/executor.py \
  eval_interactive/tests/test_report_generators.py \
  docs/foundational/phase5_evaluation_design.md
git diff --shortstat -- <same paths>
```

**Files NOT staged** (deliver-agent / human bundle at close commit per `iteration_governance.md` §8.7 / `doc_governance.md` "10-handoff retention rule"):

- `docs/milestone_objective.md` (deliver-agent — milestone-level scope + Codex review plan; not modified by S1 dev)
- `docs/sprint_objective.md` (deliver-agent — S1 contract is already on disk; archived to `docs/sprints/sprint-050-objective.md` at close by deliver-agent)
- `docs/10-handoff.md` (deliver-agent — §0 cold-start table + §1 lead refresh at close)
- `docs/action_bank.md` (deliver-agent — `R-eval-report-observability` consumption + §6 row at close)
- `docs/codex-findings.md` (review agent — milestone-shared Codex at M5 close per `milestone_objective.md` §8)
- `compact/sprint-050-dev-prompt.md` (deliver-agent — already on disk as the brief consumed by this dev session)

**Re-rendered artefact (eyeball-only per `sprint_objective.md` #5)**: `eval_interactive/results/20260523-075141/report.html` was regenerated from the unchanged `results.json` using the new renderer. **NOT staged** — `eval_interactive/results/*` is gitignored (`.gitignore:27`; M4-Eval-Cleanup never committed it either). The file exists locally for human inspection; the eyeball-verification evidence is recorded in §6.4 below.

## 3. §3 layer-classification + anti-hardcode self-walk

The §A stanza in `docs/sprint_objective.md` holds against the actual commit:

- **Target failure layer**: `infra` (eval-harness report-rendering layer) — primary. + a single intentional **foundational docs fold-back** (`phase5_evaluation_design.md §6.9`). The §3 layer classification walk in `iteration_governance.md` lands cleanly on `infra` (the eval-harness display layer; no Tier-0 invariant, no LLM-projection change, no skill-state, no semantic_planner, no CaseSpec / judge change, no product-policy). Per `iteration_governance.md` §7, S1 is **§7-exempt** (display + docs-only; no prompt, runtime semantic decision, eval-spec scoring, or judge calibration change).
- **Tier-0 invariant**: This sprint adds NO Tier-0 invariant. The Tier-0 safety floor in `docs/runtime_freeze_and_risk_policy.md` §1 / §2 is UNTOUCHED. The Tier-0 *display* surface (the new `_tier0_section` block) renders existing L1 hard-check results — which checks run and how they flip `case_passed` is unchanged.
- **Semantic hardcode**: NONE. All new logic is display-only:
  - The `suite_authority` flag is derived from existing per-case `case_passed_authority` values via the existing `sets.is_human_judgment_suite(...)` helper (which itself reads the existing `_OPT_IN_SETS` tuple). No new keyword / regex / enum is introduced. No CaseSpec id / trace phrasing / visible-eval text appears in the renderer.
  - The tier-labelled badges (`T0`/`T1`/`T2`) render existing `case_passed_authority`, `tier2_result`, and L1 result fields. No new gating logic is added.
  - The Phase-5 §6.9 dashboard rendering is **removed** (not refactored into another deterministic branch). The aggregates underneath stay computed; only the HTML is stripped.
  - The phase5 §6.9 fold-back **preserves §6.9 content** per `doc_governance.md` and adds a supersession block + cite list; no other phase5 section is rewritten.
- **Generalization coverage**: per `sprint_objective.md` §A → target = the M4 close run `results/20260523-075141` (12 bad cases, all human_review); neighbor = the same data shape under a programmatic-gate suite, exercised by the existing `_make_case_results()` fixture in `test_report_generators.py` + the new `TestSuiteAuthorityResolution` mixed-suite case; negative / shadow = N/A (no semantic decision in scope; the gate touched is visual presentation, not bot behaviour or judge calibration).

### §4.1 nine-question anti-hardcode kernel self-walk

Per `iteration_governance.md` §4.1, walked against the S1 staged diff. **Expected verdict (per sprint_objective.md §A): clean `approve` — display + docs-only exemption.**

1. **Q1 (keyword / regex / if-else / enum / per-UC matrix for a semantic decision)** — NO. The renderer adds no semantic branching. The four-tier surface reads existing per-case fields (`case_passed_authority`, `tier2_result`, `l1_results`, `l2_results`, `l3_results`); the only "decision" the renderer makes is which English label to print for each tier badge, which is a presentation choice over already-decided semantic outcomes. The `_resolve_suite_authority` aggregation is set arithmetic over `case_passed_authority` strings — no keyword / regex.
2. **Q2 (justified as protecting Tier-0)** — N/A (Q1 = no). The renderer does not introduce a new gate.
3. **Q3 (could be projected as soft signal instead)** — N/A (Q1 = no). The `suite_authority` flag is itself a soft signal projected from the existing `case_passed_authority` per-case projection.
4. **Q4 (encodes visible-eval case text / trace phrasing / CaseSpec id)** — NO. The new code references zero case_ids, zero trace phrases, zero CaseSpec identifiers. The renderer's strings are tier labels (`"Tier-0 Safety Floor"`, `"HUMAN_REVIEW"`), authority labels (`"human_review"`, `"programmatic"`, `"mixed"`), and section headers — none are eval-case-specific. The pointer string `"eval_interactive/case_specs/bad_cases/_manifest.md"` references a documented governance artefact path, not a case spec.
5. **Q5 (moves semantic ownership LLM → Java)** — NO. `server/src/main/java/**`, `server/src/main/resources/system_prompt.txt`, Skill YAMLs, prompt-projection logic, and runtime decision code are ALL UNTOUCHED. `git diff --stat -- server/` returns empty. The LLM-side semantic surface is identical before/after S1.
6. **Q6 (adds if-else block to prompt)** — NO. `server/src/main/resources/system_prompt.txt` UNTOUCHED.
7. **Q7 (preserves tool schema, capability / permission boundary, PII / safety floor, grounding floor)** — YES. Tool schema unchanged (no `customer_service_tool_spec_v0_3.{md,yaml}` edit; no tool-dispatch code touched). Capability / permission boundary unchanged. PII / safety floor — Tier-0 L1 hard checks compute and gate exactly as before; the renderer just labels them as Tier-0. Grounding floor (`faq_grounding_contract.md` six output classes, citation diagnostics) UNTOUCHED.
8. **Q8 (ships generalization eval coverage — target / neighbor / negative / shadow)** — N/A in the LLM-semantic sense (S1 is §7-exempt). Coverage at the renderer level is the 26 new unit tests + the re-render eyeball verification on the M4 run.
9. **Q9 (if temporary, has explicit rollback / sunset)** — N/A. The change is not "temporary"; the four-tier verdict surface is the new permanent renderer state. The §6.9 fold-back is permanent. No sunset plan needed.

**Verdict (self-assessment)**: `approve` with exemption note "Pure display rewrite + foundational fold-back; no semantic surface". The PR adds no semantic hardcode; the renderer reads existing semantic outcomes and re-labels them. Final §4.1 review is deferred to the M5 milestone-shared Codex pass per `milestone_objective.md` §8 (S1 + S2 + S3 [+S4] reviewed together at M5 close).

## 4. Tests run + delta vs baseline

### 4.1 Item-specific tests (post-S1, HEAD = working tree)

```
cd eval_interactive && uv run python -m pytest tests/test_report_generators.py -v
```

Result: **41 passed in 0.40s**.

- 15 pre-existing tests: ALL PASS (1 lightly updated for new badge shape — `test_html_contains_pass_fail_badges` now asserts `>PASS<` / `>FAIL<` rather than the bare `"PASS"`/`"FAIL"` substring, since the binary span was replaced by tier-labelled badges containing the same words).
- 26 new tests across 5 new test classes: ALL PASS.

### 4.2 Full Python suite

```
cd eval_interactive && uv run python -m pytest --tb=no -q
```

Result: **3 failed, 486 passed in 13.13s**.

Comparison vs baseline (`docs/10-handoff.md` §0: `3 failed, 460 passed` at HEAD `d5b1508` / `84ae017`):

| Metric | Baseline | Post-S1 | Delta |
|---|---|---|---|
| failed | 3 | 3 | **0** (no new failures) |
| passed | 460 | 486 | **+26** (= the 26 new tests in `test_report_generators.py`) |

The 3 residual failures are the env-specific pre-existing failures documented at `docs/10-handoff.md` §0 (OQ-S47.3 — venv `pytest` console-script shebang / corpus_lint module discovery) and are explicitly out of scope for S1:

```
FAILED tests/regression/test_case_spec_overrides.py::test_v2_schema_loads_cleanly
FAILED tests/regression/test_case_spec_overrides.py::test_smoke_review_report_tracks_smoke_set_and_overrides
FAILED tests/regression/test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag
```

Reproducible commands:

```bash
cd eval_interactive
uv run python -m pytest tests/test_report_generators.py -v
uv run python -m pytest --tb=no -q
```

### 4.3 Java + real-LLM run

Per `sprint_objective.md` §"Test / eval requirements": **no Java run required** (zero `server/` touch) and **no real-LLM rerun required** (no bot-behaviour or judge change; S1 is display + docs). Java baseline `1163 / 1-inherited / 0 / 2` UNCHANGED by construction.

## 5. Phase-5 §6.9 fold-back diff (item #4)

The fold-back is the one intentional foundational doc edit this sub-sprint. Two surgical edits:

1. **Front-matter added** at the top of `docs/foundational/phase5_evaluation_design.md` (the document carried no YAML front matter pre-S1):

   ```yaml
   ---
   title: Phase 5 — Evaluation Design (V1)
   doc_tier: foundational
   status: partial
   implementation_status: partial
   source_of_truth: this file (with §6.9 superseded by docs/milestones/M3-Eval_objective.md + docs/current/iteration_governance.md §5.5/§5.6 — see §6.9 supersession block)
   last_reviewed: 2026-05-24
   review_cadence: every 3-5 sprints
   supersedes: []
   superseded_by: null
   notes: >
     Front matter added 2026-05-24 (Sprint 50 / M5 S1) when §6.9 ...
   ---
   ```

2. **Supersession block** inserted immediately before the `### 6.9 Top-Line Metrics (7 Key — Interactive + Replay)` heading. The block names:
   - `status: superseded` (for §6.9 only — rest of doc remains `current-or-partial`).
   - `implementation_status: historical` (7-metric framing was pre-M3).
   - Three superseding artefacts: `docs/milestones/M3-Eval_objective.md` (four-tier pyramid), `iteration_governance.md` §5.5 (smoke composite_score demoted), `iteration_governance.md` §5.6 (curated bad-case suite as primary human-judgment gate).
   - Explicit note that §6.9 content is **PRESERVED** per `doc_governance.md`'s "preserve forward-looking design" + "never rewrite foundational specs sprint-by-sprint" rules.
   - Explicit note that the 5 implemented aggregates (`task_success_rate`, `stall_rate`, `policy_compliance_rate`, `mean_turns_to_resolution`, `mean_composite_score`) STAY computed by `_compute_summary` and STAY in `results.json` for downstream consumers — only the HTML 7-card dashboard *rendering* is removed.
   - Explicit note that the 3 N/A metrics (`correct_tool_invocation_rate` / `escalation_correctness_rate` / `grounded_final_answer_rate`) remain out of scope for S1 per the hard fence.

No other phase5 section is rewritten. The 7-metric table and surrounding §6 / §7 / §8 / §15 sections of phase5 are byte-identical to pre-S1.

Reproducible command:

```bash
git diff -- docs/foundational/phase5_evaluation_design.md
```

**Numbers**: 34 insertions, 0 deletions on phase5. The fold-back is purely additive (front matter + supersession block); no §6.9 content is deleted or rewritten.

**Flagged for Codex**: this is the ONE foundational doc edit in S1 and is a *stated intentional fold-back* per `doc_governance.md` "Fold-back cadence" + `sprint_objective.md` §"Scope" #4. The corresponding reconciliation note (the supersession block itself + this §5 of the handoff) names the intent. Codex review per `iteration_governance.md` §4 should verify (a) the fold-back is minimal (front matter + one block), (b) §6.9 content is preserved (no deletion), (c) no other phase5 section is rewritten.

## 6. Per-item walk-through

### 6.1 Item #1 — Four-tier HTML verdict surface

Built per `sprint_objective.md` #1. Key shape:

- **Header** — `_header_section`: renders run_id / label / timestamp / total cases + a `<div class="suite-authority-badge {cls}">` block (CSS variants `human-review` / `programmatic` / `mixed`) carrying the resolved authority + a one-line guidance note. On `human_review` the note explicitly names "Programmatic case_passed counts below are informational, not the gate" + a `<code>` pointer to `eval_interactive/case_specs/bad_cases/_manifest.md`. On `programmatic` the note explains the L1 ∧ mandatory-L2 ∧ Tier-2-not-critical-failed gating semantics.
- **Tier-0 Safety Floor** — `_tier0_section`: aggregates L1 hard-check results across cases by `check_name`. Renders a "Cases clean: X/Y" stat + a per-check passed/total table. Tier-0 violations surface as a `cases_with_any_fail > 0` headline. (Display nuance, optional per dev prompt §3 #1: the 2 known false-positive `no_pii_leakage` regex hits on cs011/cs012 are surfaced **without** a filter — the proposal §10 Risk 2 mitigation is informational rendering only and the dev prompt says it is *optional*; we did not add the sub-annotation because (a) introducing a "known false positive" exception table is itself a semantic decision that belongs to the L1 check or to the human-review manifest, not to the renderer, and (b) the manifest pointer in the Tier-1 section already points the human at the human-review verdict that classifies them. Surfacing it without filter satisfies the hard fence "no filter on the underlying check".)
- **Tier-1 Outcome** — `_tier1_section`: branches on `suite_authority` for the headline. On `human_review`: "Human-judgment suite — human review of `closure_criterion` against `per_turn_trace` is the primary gate"; surfaces `Programmatic case_passed (informational): N/T` + the manifest pointer. On `programmatic`: "Programmatic suite — `case_passed` is the gate"; surfaces `case_passed: N/T` + the gate semantics. On `mixed`: "Verdict varies per case authority"; lists the per-authority split.
- **Tier-2 Critical-flow** — `_tier2_section`: aggregates per-case `tier2_result.passed` × `severity` across cases. Renders PASS / FAIL-critical / FAIL-advisory / N/A counts + a "Critical Tier-2 failures" table (case_id → `failed_step_ids`) + an "Advisory Tier-2 failures (informational)" table. Per-case Tier-2 detail surfaces in the per-case drawer via `_render_tier2`.
- **Tier-3 Polish (advisory)** — `_tier3_section`: aggregates mean L2 outcome / mean L3 judge / stall rate (the still-computed aggregates from `_compute_summary`) + a per-L3-dimension mean table. The tier-note explicitly names the demoted dims (`relevance` / `tone_appropriateness` / `groundedness` per S-Eval-5; `handover_completeness` / `case_id_present` per S-Cleanup-3 #4) and the Tier-1 supplementary advisory (`user_goal_achievement`) so the human is not confused about which advisories were demoted from what.
- **Programmatic-suite per-UC rollup** — `_per_uc_table`: **kept** per `sprint_objective.md` #2 ("not deleted"); **reframed** with the section header "Per Use-Case Breakdown (programmatic-suite rollup)" and a tier-note saying it is informational only on human-judgment suites. The `task_success_rate` column was removed (the underlying `_compute_summary` per-UC dict doesn't carry it; the previous code rendered "N/A").
- **Per-case details** — `_render_case`: each case is a `<details>` block. The `<summary>` now carries three tier-labelled badges (`T0 {label} · T1 {label} · T2 {label}`) instead of the single binary PASS/FAIL badge. The drawer carries the existing key-value grid (with `case_passed_authority` + `case_passed (programmatic)` rows added so the distinction is explicit) + Tier-0 L1 checks + the NEW Tier-2 block (via `_render_tier2`) + Tier-3 L2 / L3 advisory tables + the failure-tag list.

CSS additions: a `.tier-box` + `.tier{0,1,2,3}` border-left coloring system (red / amber / blue / gray), a `.suite-authority-badge` block, a `.badge.tier` styling for the tier-labelled per-case badges + a `.badge.human-review` and `.badge.na` color variant. No external CSS dependency added; the report stays self-contained.

### 6.2 Item #2 — Phase-5 §6.9 7-metric dashboard rendering removed

Per `sprint_objective.md` #2 + the human's Alternative B choice:

- `_metrics_dashboard` (the pre-S1 method at ~lines 318-355 rendering the 7 `<div class="card">` blocks): **DELETED**.
- `_pass_fail_overview` (the pre-S1 method at ~lines 357-402 rendering the "Passed: N / Failed: N; Mean Composite: …" headline + composite-score-distribution table): **DELETED**.
- The pre-S1 `generate()` method's hardcoded section ordering invoked both methods immediately after the header; post-S1 the section ordering invokes the four tier sections + the reframed per-UC table instead.
- The `_per_uc_table` is **KEPT but REFRAMED** (per `sprint_objective.md` #2 "kept but reframed under the four-tier view, not deleted"). Header re-labelled; tier-note added; `task_success_rate` column dropped (it was N/A under the existing per-UC summary dict shape).

What stays (backward-compat — the data is intact in `results.json`):

- `_compute_summary` continues to compute `passed_cases` / `failed_cases` / `task_success_rate` / `stall_rate` / `mean_composite_score` / `mean_outcome_score` / `mean_judge_score` / `escalation_correctness` / `policy_compliance_rate` / `mean_turns_to_resolution` / `per_uc_breakdown`. All STAY in the JSON summary. The Tier-3 section consumes the mean-outcome / mean-judge / stall-rate signals; the per-UC table consumes the per_uc_breakdown.
- `json_report.py::_build_summary` continues to emit the same legacy 7-metric framing keys (`task_success_rate`, `policy_compliance_rate`, `mean_turns_to_resolution`, `mean_composite_score`, etc.) so external consumers of `results.json` (the bad-case manifest, the codex review pass, etc.) do not break. The NEW `suite_authority` field is the only summary-block addition.

### 6.3 Item #3 — `suite_authority` aggregate flag (single source of truth)

Per `sprint_objective.md` #3:

- Computed **once** in `executor._compute_summary` from per-case `case_passed_authority` annotations (which themselves come from `_resolve_case_passed_authority(case_spec)` upstream — already wired in S-Cleanup-2 from `CaseSpec.source_suite` via `is_human_judgment_suite`).
- Aggregation rule: `{"human_review"}` → `"human_review"`; `{"programmatic"}` → `"programmatic"`; otherwise (mixed or any unknown value) → `"mixed"`. Empty case list → `"programmatic"` (the safety default per the per-case resolver).
- Unannotated per-case rows (e.g., loaded from a pre-S-Cleanup-2 `results.json`) coalesce to `"programmatic"` via the `r.get("case_passed_authority") or "programmatic"` expression — matches the safety default the per-case resolver uses (covered by `TestComputeSummarySuiteAuthority::test_compute_summary_unannotated_coalesces_to_programmatic`).
- Consumed by **both** renderers: `html_report.py::_resolve_suite_authority` (prefers summary field, falls back to per-case derivation for historical re-renders) and `json_report.py::_build_summary` (emits the field for external consumers). The two renderers do **not** duplicate aggregation logic — they both read the single source-of-truth field computed by `_compute_summary` (proposal §11 risk mitigation).

The hard fence "do not alter `_compute_summary`'s existing `passed_cases` / `task_success_rate` *computation* — only ADD" is verifiable from the diff: the only changes inside `_compute_summary` are (a) the new field in the empty-case-list early-return branch, (b) the new aggregation block before the main return, (c) the new field in the main return. Lines 855-859 (the `passed` / `failed` computation), lines 869-894 (the per-UC breakdown), lines 897-926 (the escalation correctness + policy compliance) are byte-identical to pre-S1.

### 6.4 Item #5 — Re-render regression sanity check

Regenerated `eval_interactive/results/20260523-075141/report.html` (the M4-Eval-Cleanup close bad-case run) from the **unchanged** `results.json` using the new renderer. `results.json` is NOT touched (verified by `git status --short eval_interactive/results/` returning empty).

Reproducible command:

```bash
cd eval_interactive
uv run python -c "
import json
from pathlib import Path
from eval_interactive.report.html_report import HtmlReportGenerator

run_dir = Path('results/20260523-075141')
data = json.loads((run_dir / 'results.json').read_text())
gen = HtmlReportGenerator()
html = gen.generate(
    run_id=data['run_id'], label=data['label'],
    case_results=data['case_results'], summary=data['summary'],
)
gen.save(html, run_dir / 'report.html')
"
```

Eyeball verification commands + outcomes:

```bash
grep -c "HUMAN-JUDGMENT\|Tier-0 Safety Floor\|Tier-1 Outcome\|Tier-2 Critical-flow\|Tier-3 Polish\|cs012_uc_fp_late_phone_failure_path\|uc-h-intake-complete-before-handover" \
  eval_interactive/results/20260523-075141/report.html
# → 11 matches across all four tier headers + critical Tier-2 case + failed step id

grep -E "Key Metrics Dashboard|Pass / Fail Overview|0/12 Passed|correct_tool_invocation_rate" \
  eval_interactive/results/20260523-075141/report.html ; echo "exit=$?"
# → exit=1 (no matches) — confirms the misleading "0/12" headline + legacy dashboard absent
```

What the re-rendered report shows (eyeball-confirmed):

- Top: `Suite authority: human_review — HUMAN-JUDGMENT suite (per §5.6) — manual review required`. Beneath: "Programmatic `case_passed` counts below are informational, not the gate — see `eval_interactive/case_specs/bad_cases/_manifest.md` for the deliver-agent + human verdict from the milestone close."
- Tier-0 Safety Floor section — 10/12 cases clean; the 2 `no_pii_leakage` regex hits surface as Tier-0 violations (per the dev-prompt §3 #1 informational-rendering nuance — they remain visible without a filter; the human reading the report at M4 close already classified them as benign in the manifest).
- Tier-1 Outcome section — "Programmatic case_passed (informational): 5/12" + manifest pointer + the human_review = 12/12 authority split.
- Tier-2 Critical-flow section — "8/12 cases pass Tier-2" + a critical-failures table (cs012, cs066, cs095, wmkb each on their respective intake/search steps) + an advisory-failures table (5 cases on `record-outcome-on-grounded-answer`).
- Tier-3 Polish section — mean outcome 0.42, mean judge 0.00, stall rate 0% + a per-L3-dim table (empty — bad-case fixtures carry no `llm_judge_dimensions` per S-Cleanup-2).
- Programmatic-suite per-UC rollup — kept + reframed.
- Per-case details — 12 `<details>` blocks each with `T0`/`T1`/`T2` tier-labelled badges (e.g., `cs012 — T0 FAIL · T1 HUMAN_REVIEW · T2 FAIL`).
- The misleading "Passed: 0 / Failed: 12 — Task Success Rate 0.0%" headline that the human cold-read at the start of M5 planning is **GONE**.

**`results.json` integrity verified**: `git status --short eval_interactive/results/` returns no modifications.

### 6.5 Item #6 — Test coverage

Per §4.1 above: 26 new tests across 5 new test classes; 15 existing tests pass (1 lightly updated for the new badge shape); 41 total in `test_report_generators.py`; full Python suite +26 net passes, zero new failures vs the documented baseline.

## 7. Trace / observability changes

S1 IS the observability work for the report surface. Two notes:

- **HTML report**: the new four-tier surface is the post-M3/M4 observability surface a human consults when reading a run cold. The pre-S1 surface (Phase-5 §6.9 dashboard + binary PASS/FAIL badge) is gone from HTML. `results.json` carries identical data shape vs pre-S1 + one new top-level summary field (`suite_authority`) + the per-case `case_passed_authority` and `tier2_result` fields are now also surfaced by `json_report.py::_build_case` (they were always in the executor's per-case dict; `json_report.py` was just not propagating them through its `_build_case` normaliser).
- **No admin-trace / per-invocation-trace changes**: S2 (`per-invocation trace`) and S3 (`projection audit + Skill-driven convergence`) are the remaining M5 sub-sprints per `milestone_objective.md` §3 and are explicitly out of S1 scope.

## 8. Open questions surfaced

- **OQ-S50.1 — Re-rendered `report.html` is gitignored.** `sprint_objective.md` §"Commit discipline" + the dev prompt §7 enumerate the re-rendered `eval_interactive/results/20260523-075141/report.html` among the S1 scope files to stage. The file is on disk (eyeball-verified per §6.4) but `eval_interactive/results/*` is gitignored (`.gitignore:27`). Force-adding via `git add -f` is technically possible but conflicts with the existing repo policy "Eval run artifacts — regenerated per run; keep the directory via .gitkeep" (the same policy applied across M3-Eval / M4-Eval-Cleanup; no prior sprint has committed a `results/*` artefact). Per the dev prompt §6.5 + §10 STOP-condition framing this is a minor contract gap, not a STOP condition. **Disposition recommendation**: do NOT force-add; rely on the §6.4 eyeball-verification record + the reproducible command to regenerate. Deliver-agent + human may revisit at S1 close.
- **OQ-S50.2 — Tier-0 informational rendering of known false-positive `no_pii_leakage` hits (cs011/cs012).** Dev prompt §3 #1 marks this as *optional* informational rendering. We chose NOT to add a sub-annotation in the Tier-0 section render because (a) introducing a "known false positive" annotation is itself a semantic decision and risks coupling the renderer to specific case_ids (Q4 of the §4.1 kernel); (b) the manifest pointer in Tier-1 already directs the human to the authoritative human-judgment verdict where these are classified benign; (c) the hard fence "do NOT add a filter on the underlying check" stays clean. Deliver-agent + human may surface a follow-on if a different convention is preferred (e.g., a manifest-driven annotation that consumes structured metadata rather than per-case hardcodes).
- **OQ-S50.3 — `_per_uc_table` `task_success_rate` column dropped.** Pre-S1 the per-UC table showed a `Task Success Rate` column that rendered N/A because `_compute_summary`'s `per_uc_summary` does not include the per-UC rate (only the global). We removed the column rather than backfill it because (a) keeping a N/A column is misleading; (b) reviving a per-UC `task_success_rate` is a `_compute_summary` change that belongs in a programmatic-suite-aware milestone, not in S1 display-only scope. No data loss — the per-UC `count` / `passed` / `failed` / `mean_composite` survive. Deliver-agent + human may surface a follow-on if per-UC TSR is wanted.
- **OQ-S50.4 — `json_report.py` per-case payload now includes `case_passed_authority` and `tier2_result`.** Pre-S1 the JSON renderer's `_build_case` did NOT propagate these fields (they were silently dropped from `cases[]` in the normalised report). Post-S1 they are surfaced. External consumers of `json_report.py`-produced JSON (vs `executor._save_results`-produced JSON, which already carried them) gain access to fields they previously did not see; this is a **strict addition** with no removal, so it is backward-compat-safe. Flagged here for visibility.

## 9. Drift items

- None new. The `outcome_checks.py` `_CASE_ID_UCS` / `_uc_family` duplicates noted at S-Cleanup-3 close (`sprint-049-handoff.md` §9) remain — orthogonal to S1.
- The pre-S1 `_metrics_dashboard` referenced three Phase-5 §6.9 metrics that were never implemented (`correct_tool_invocation_rate` / `escalation_correctness_rate` / `grounded_final_answer_rate`). Removing the dashboard removes the misleading "N/A" rendering of these metrics; the underlying drift (definitions in phase5 §6.9 vs absent `_compute_summary` implementation) is acknowledged in the new phase5 §6.9 supersession block.

## 10. Hard fence honored checklist

Per `docs/sprint_objective.md` §"Hard fences / STOP conditions":

- [x] No `composite.py::compute_composite` or scoring-weight or `>= 0.7` summary-threshold change — confirmed by `git diff --stat -- eval_interactive/eval_interactive/scoring/composite.py` returning empty.
- [x] Tier-2 HTML display is NOT coupled to the §5.6 human-judgment gate — `_tier2_section` aggregates `tier2_result` independent of `suite_authority`; the rendering is observation, not gating logic.
- [x] No `_resolve_case_passed_authority` semantics or `_OPT_IN_SETS` membership change — `git diff --stat -- eval_interactive/eval_interactive/batch/sets.py` returns empty; the `_resolve_case_passed_authority` function in `executor.py` (lines 43-66) is byte-identical pre/post-S1.
- [x] No bad-case (or any) fixture YAML edit — `git diff --stat -- eval_interactive/case_specs/` returns empty.
- [x] No `_compute_summary` existing-field computation change — only the additive `suite_authority` field was added; `passed_cases` / `task_success_rate` / etc. computation lines are byte-identical (see §6.3 above).
- [x] No `server/**` touch — `git diff --stat -- server/` returns empty.
- [x] No `eval_interactive/case_specs/shadow/` reads — dev session did not open any shadow file.
- [x] No historical `results.json` regeneration — `git status --short eval_interactive/results/` confirms `results.json` UNCHANGED; only `report.html` (gitignored) was rewritten on disk for eyeball verification.
- [x] No deletion or rewrite of phase5 §6.9 content — only front matter + a supersession block were added; the 7-metric table and all subsequent §6 / §7 / §8 / §15 sections are byte-identical.
- [x] No implementation of the three N/A Phase-5 metrics — confirmed by `git diff` showing no new metric computation; the new phase5 §6.9 supersession block explicitly documents this is out of scope.
- [x] No `docs/sprints/*` edit (except NEW `sprint-050-handoff.md` — this file).
- [x] No `docs/milestones/*` edit.
- [x] No `docs/10-handoff.md` / `docs/action_bank.md` / `docs/milestone_objective.md` / `docs/sprint_objective.md` / `docs/codex-findings.md` edits — deliver-agent / review-agent territory.
- [x] No STOP-condition surfaced — removing the dashboard did not reveal any downstream consumer of the removed HTML (the only documented consumer was the report itself; no doc / runbook / code references the deleted DOM); rendering the four-tier view did not require any fixture or scoring change (it reads existing per-case fields).

## 11. R-item flip request

**Requested CLOSURE at S1 / M5 close** (deliver-agent + human discretion):

- `R-eval-report-observability` (`docs/action_bank.md:779`, opened S-Cleanup-2 close 2026-05-23) — S1 consumes all three sub-items:
  - (a) `_resolve_case_passed_authority` surfaces `case_passed_authority` per-case but does NOT propagate to `RunResult.summary` as a top-level `suite_authority` aggregate flag — **DELIVERED** (item #3 above; `_compute_summary` now emits `suite_authority`).
  - (b) HTML report template does NOT render `case_passed_authority` — **DELIVERED** (item #1 above; `_render_case` carries per-case `case_passed_authority` row + Tier-1 section + tier-labelled badges; on `human_review` suites the badge surfaces `HUMAN_REVIEW`).
  - (c) Post-M3-Eval cleanup audit P2 finding "HTML report 不显示 Tier-2 / severity 信息" — **DELIVERED** (item #1 above; NEW `_render_tier2` + `_tier2_section` aggregate render `passed` / `severity` / `failed_step_ids` per-case + aggregate).

Deliver-agent applies the actual R-item flip in `docs/action_bank.md` at close per the close-bundle convention.

## 12. Closure verdict (deliver-agent + human)

**Verdict: A — Clean PASS** (deliver-agent + human, 2026-05-24). First sub-sprint
of Milestone M5 — Observability Coherence; closed clean; milestone continues with S2.

**Deliver-agent independent verification at close** (not dev self-report):

- Commit `9ef9d1e`, parent `84ae017` (M4 close); 6 files, 1458(+)/188(-).
- **Hard fences fence-verified** (`git show 9ef9d1e --numstat -- <path>` empty):
  no `server/*`; no `composite.py` / `sets.py` / `case_specs/*`; no deliver-agent
  files swept into the commit (`milestone_objective` / `sprint_objective` /
  `10-handoff` / `action_bank` / `codex-findings` / `compact/*` all absent —
  clean `git add` discipline, not `git add -A`).
- phase5 §6.9 fold-back = `34(+)/0(-)` — purely additive; §6.9 content preserved
  per `doc_governance.md`.
- **Tests independently reproduced** by deliver-agent: `test_report_generators.py`
  41 passed; full Python suite `3 failed / 486 passed` (+26 vs the `3 failed /
  460 passed` baseline; the 3 residual are the env-specific
  `test_case_spec_overrides`×2 + `test_corpus_lint`×1 per OQ-S47.3, NOT S1
  regressions). Java `1163 / 1-inherited / 0 / 2` UNCHANGED by construction.
- Re-rendered `results/20260523-075141/report.html`: four-tier surface +
  `suite_authority: human_review` badge present; the misleading "0/12 passed"
  headline gone (dev §6.4 grep evidence; file gitignored).

**§4.1 anti-hardcode**: S1 is §7-exempt (display + docs-only); Codex DEFERRED to
the M5 milestone-shared pass per `iteration_governance.md` §4.3 (no per-sub-sprint
trigger). Self-walk clean `approve` (handoff §3) accepted.

**OQ dispositions (all note-only; no fix-iteration):**

- **OQ-S50.1** (re-rendered `report.html` gitignored, not staged) → ACCEPT dev
  recommendation: do NOT force-add; `eval_interactive/results/*` is gitignored
  repo-wide (no prior sprint committed a results artefact); the reproducible
  regen command + the §6.4 eyeball record suffice.
- **OQ-S50.2** (Tier-0 false-positive `no_pii_leakage` rendering, cs011/cs012) →
  ACCEPT dev's no-annotation choice. Surfacing the hits without a filter is
  correct — a "known-FP" annotation would couple the renderer to case_ids (a
  §4.1 Q4 risk); the Tier-1 manifest pointer already routes the human to the
  authoritative human-judgment verdict.
- **OQ-S50.3** (per-UC `task_success_rate` column dropped) → ACCEPT. The column
  rendered N/A under the existing per-UC summary shape; reviving a per-UC TSR is
  a `_compute_summary` change out of S1 display scope. No data loss.
- **OQ-S50.4** (`json_report.py` per-case now surfaces `case_passed_authority` +
  `tier2_result`) → ACCEPT. Strict additive; backward-compat-safe.

**R-item**: `R-eval-report-observability` → **CLOSED 2026-05-24** (all three
bundled sub-items delivered). Flip applied in `docs/action_bank.md` at close.

**Next**: M5 continues with **S2 (Sprint 51) — Per-invocation trace (B2)** per
`docs/milestone_objective.md` §3 ordering (S1 independent → **S2 before S3** → S3
[+ S4]). S2 contract pending the deliver-agent + human storage-shape decision.
