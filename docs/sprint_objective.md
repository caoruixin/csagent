---
title: Sprint 48 / S-Cleanup-2 — Bad-case fixture schema unification + executor suite-mode
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (current sub-sprint contract)
last_reviewed: 2026-05-23
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-047-objective.md]
superseded_by: null
notes: >
  Second sub-sprint of Milestone M4-Eval-Cleanup. Layer: `eval_spec`
  (Alice fixture schema unification — Item #5) + `infra` (executor
  suite-mode annotation — Item #2). §7 stanza required (eval_spec
  touch). Codex per-sub-sprint trigger NOT expected (default: milestone-
  shared at M4-Eval-Cleanup close per `iteration_governance.md` §4.3).

  Two scope items consumed from the post-M3-Eval cleanup audit
  (verified by deliver-agent 2026-05-23):
  - #5 Alice bad-case fixture schema inconsistency — Alice retains
    full legacy `outcome_checks` (7 items) + `llm_judge_dimensions`
    (3 items) while the other 11 bad cases have empty lists per
    M3-Eval design intent. Default direction (per human approval
    2026-05-23): strip Alice's legacy `outcome_checks` + `llm_judge_
    dimensions` lists; PRESERVE `closure_criterion` + load-bearing
    `bad_case_metadata` (source_session_id, surfaced_by, surfaced_
    date, failure_shape, expected_behavior).
  - #2 Executor has no "suite mode" — same scoring logic for all
    suites. For `bad_cases/` + `anchor_outcome/` (the §5.6 human-
    judgment suites), report output should annotate explicitly that
    programmatic `case_passed` PASS/FAIL is NOT the gate; human
    review of `closure_criterion` against `per_turn_trace` is the
    gate.

  One R-item closed: `R-bad-case-fixture-migrate-to-l3-judge-dims`
  (Item #5).
---

# Sprint 48 / S-Cleanup-2 — Bad-case fixture schema unification + executor suite-mode

## 1. Sprint identity

- **Sprint number**: 48 (global) / S-Cleanup-2 (M4-Eval-Cleanup sub-
  sprint 2).
- **Milestone**: M4-Eval-Cleanup (`docs/milestone_objective.md`).
- **Branch**: `refactor/remove-the-shackles` (continuing).
- **HEAD prior to dev**: TBD — deliver-agent's S-Cleanup-1 close-
  bundle commit on top of dev's `1576070` (S-Cleanup-1).
- **Estimated duration**: 1-2 dev days.

## 2. Goal

Resolve Alice bad-case fixture schema inconsistency (Item #5) and add
executor suite-mode awareness so bad_cases / anchor_outcome suite runs
produce reports that make their human-judgment-gate nature explicit
(Item #2). Close R-item `R-bad-case-fixture-migrate-to-l3-judge-dims`.

This sub-sprint preserves the M3-Eval four-tier evaluation pyramid +
§5.6 human-judgment gate; it adds NO new semantic surface and changes
NO governance text.

## 3. Layer-classification + anti-hardcode stanza (per §7)

**Target failure layer**: `eval_spec` (primary — Alice fixture
schema) + `infra` (secondary — executor / report suite-mode
annotation).

**Tier-0 invariant**: This sprint adds no Tier-0 invariant. Existing
Tier-0 invariants in `docs/runtime_freeze_and_risk_policy.md` §1 / §2
unchanged.

**Semantic hardcode**: No semantic hardcode introduced. (a) Stripping
Alice's `outcome_checks` + `llm_judge_dimensions` is a fixture-data
edit on a single YAML, reflecting design-intent alignment; it removes
data, not logic. (b) The executor suite-mode annotation is a report
metadata flag (e.g., `case_passed_authority: "human_review" |
"programmatic"`) that surfaces existing governance state per §5.6;
the flag is determined by suite name (directory membership), not by
semantic content of any case.

**Generalization coverage**: Not applicable — this sub-sprint adds no
new semantic surface. Acceptance is per-item verification per §6.

## 4. Files in scope

| # | Item | Files | Expected change | Complexity |
|---|------|-------|-----------------|---|
| 5 | Alice bad-case fixture schema unification | `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` | Strip Alice's `outcome_checks` list (7 items → empty) + `llm_judge_dimensions` list (3 items → empty) to align with the 11-case M3-Eval empty-list schema. **PRESERVE** `closure_criterion` (the §5.6 human-judgment-gate input); **PRESERVE** load-bearing `bad_case_metadata` fields (`source_session_id`, `surfaced_by`, `surfaced_date`, `failure_shape`, `expected_behavior`); **PRESERVE** all other Alice CaseSpec fields (case_id, primary_uc, secondary_ucs, expected, scoring excluding the two stripped lists, turns / persona / etc.). | LOW (1 YAML file; mechanical edit) |
| 2 | Executor suite-mode awareness — annotate `bad_cases/` + `anchor_outcome/` report output as human-judgment | `eval_interactive/eval_interactive/batch/executor.py` (`_build_case_result` and / or `CaseResult` schema); possibly `eval_interactive/eval_interactive/batch/sets.py` (cross-ref opt-in sets to drive the annotation); possibly `eval_interactive/eval_interactive/case_spec/schema.py` (if a `case_passed_authority` field is added to the `CaseResult` schema) | Add a `case_passed_authority: "human_review" \| "programmatic"` field (or equivalent) on `CaseResult` (or on the result JSON's per-case object). For cases originating from `bad_cases/` or `anchor_outcome/` (cross-ref `_OPT_IN_SETS` from sets.py per S1 convention), set authority = `"human_review"` and add an explicit "human review required per §5.6" annotation in stdout. For cases from other suites, set authority = `"programmatic"` (existing behavior). | LOW-MEDIUM (1-3 files; schema field + executor branching) |

**Excluded from S2** (carried to S3):
- #4 `handover_completeness` / `case_id_present` demotion → S3.
- #9 Tier-2 design decision → S3.

## 5. Files NOT in scope (hard fences)

- `server/src/main/java/**` — no runtime code touch.
- `server/src/main/resources/skills/*.yaml` — no Skill YAML edits.
- `server/src/main/resources/system_prompt.txt` — no edits.
- `docs/runtime_freeze_and_risk_policy.md` — no edits.
- `docs/current/iteration_governance.md` — no edits in S2 (S3 may touch
  §5.6 governance text).
- `docs/current/doc_governance.md` — no edits.
- `docs/current/agent_context_guide.md` — no edits.
- `docs/sprints/sprint-NNN-*` — immutable archives (only NEW
  `docs/sprints/sprint-048-handoff.md` created at S2 close by dev).
- `docs/milestones/*` — immutable archives.
- `docs/10-handoff.md` — deliver-agent maintains.
- `docs/action_bank.md` — deliver-agent maintains R-item flips at sub-
  sprint close; no dev edits.
- `docs/codex-findings.md` — review-agent territory; no dev edits.
- `docs/milestone_objective.md` — set at milestone planning; no dev
  edits.
- `eval_interactive/case_specs/bad_cases/cs*.yaml` (other 11) — S2
  scope is Alice only; do NOT touch the other 11 to "match" Alice by
  re-adding fields (per milestone §3 default direction).
- `eval_interactive/case_specs/bad_cases/fg5q*.yaml` + `iwzx*.yaml` +
  `wmkb*.yaml` — same as above (no touches).
- `eval_interactive/case_specs/bad_cases/_manifest.md` — no edits in
  S2 (manifest update at close is deliver-agent territory if needed).
- `eval_interactive/case_specs/shadow/` — held-out, dev-blind.
- `eval_interactive/eval_interactive/scoring/composite.py` — S3
  territory (`handover_completeness` / `case_id_present` demotion).
  **EXCEPTION**: if Item #2's `case_passed_authority` field requires
  composite.py to also surface the authority on `CompositeScore`,
  that touch IS in scope (narrow extension), but no S3-territory
  changes.
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
  — S3 territory.
- `eval_interactive/eval_interactive/batch/executor.py:365-392`
  (`_compute_tier2_result`) — S3 territory.

## 6. Success metrics

Per-item verification (each item must individually verify before sub-
sprint close):

- **#5 Alice fixture stripped + preserved correctly**:
  - `grep -A 3 "^outcome_checks:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` shows empty list (`outcome_checks: []` or equivalent YAML empty-sequence representation).
  - `grep -A 3 "^llm_judge_dimensions:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` shows empty list.
  - `grep -A 5 "closure_criterion:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` shows the SAME content as pre-S2 (UNCHANGED).
  - `grep -A 8 "bad_case_metadata:" eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` shows ALL 5 load-bearing fields UNCHANGED.
  - End-to-end: `cd eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_spec; from pathlib import Path; cs = load_case_spec(Path('case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml')); print(cs.case_id, cs.scoring.outcome_checks, cs.scoring.llm_judge_dimensions)"` shows empty lists + case still loads.
- **#2 Executor suite-mode annotation**:
  - `eval-interactive run --set bad_cases --parallel 1` (smoke) produces a `results.json` where every per-case object carries `case_passed_authority: "human_review"` (or equivalent).
  - `eval-interactive run --set anchor --parallel 1` (smoke on a small subset, e.g., 1-2 cases) produces `results.json` with `case_passed_authority: "programmatic"` for those cases.
  - Stdout for `--set bad_cases` includes an explicit "human review per §5.6 required" annotation per-case OR per-run header (dev's discretion).
- **Bad-case suite regression-safety** (running directly to confirm Alice strip doesn't affect closure_criterion judgment):
  - `eval-interactive run --set bad_cases --parallel 1` against post-S2 HEAD; deliver-agent + human review Alice's `per_turn_trace` + `closure_criterion` evidence post-close to confirm Alice still surfaces the UC-A↔UC-H mis-classification failure shape (qualitatively unchanged from M3-Eval close).

Sprint-level:
- **R-item flips** (deliver-agent does the flip at close):
  - `R-bad-case-fixture-migrate-to-l3-judge-dims` → **CLOSED** (Item #5 delivers the migration in the strip direction).
- **Java baseline unchanged** — `1163 / 1-inherited / 0 / 2` at S2 close.
- **Python baseline preserved or improved** — no NEW failures from S2 (Item #2 may add NEW tests for the suite-mode annotation; those should PASS).
- **Dev handoff** at `docs/sprints/sprint-048-handoff.md` with §1-§11 complete; §12 reserved for deliver-agent + human close classification.

## 7. Stop conditions

- **If S2 planning finds Alice strip direction is wrong** (i.e., the 11 bad cases should grow legacy fields instead of Alice stripping), halt and surface — this is a milestone §10 stop condition. Most likely scenario: dev reads M3-Eval design docs + S-Eval-4 close artefact and finds the empty-list schema was a TEMPORARY calibration-dry-run shape, NOT permanent design intent.
- **If stripping Alice would require removing or weakening `closure_criterion` or load-bearing `bad_case_metadata`** (source_session_id, surfaced_by, surfaced_date, failure_shape, expected_behavior), halt — this is a §10 stop condition per milestone §3 S2 paragraph. Those fields are §5.6 human-judgment-gate inputs and must be preserved regardless of unification direction.
- **If post-S2 bad-case suite rerun shows REGRESSION on Alice's qualitative failure-shape evidence** (e.g., the UC-A↔UC-H mis-classification no longer surfaces in `per_turn_trace`), halt and re-scope — Alice's regression-guard purpose must be preserved.
- **If Item #2 executor suite-mode requires plumbing that crosses S3 territory** (e.g., changes to `composite.py` Tier-2 wiring or `skill_procedure_check.py`), halt and surface — S3 reserves those surfaces.
- **If executor `case_passed_authority` schema addition requires changes to OTHER eval-side schema files beyond the in-scope set** (e.g., a database migration, a third-party consumer schema), halt and surface — likely indicates scope expansion.

## 8. Codex review plan

Per `iteration_governance.md` §4.3 default: **deferred to M4-Eval-
Cleanup milestone close**. No per-sub-sprint Codex trigger expected
(this is eval_spec fixture + infra annotation; no Tier-0 candidate;
no §1.7 cross; no hard-fence violation; no fix-iteration on prior
sub-sprint).

## 9. Generalization coverage stanza

Not applicable — see §3 stanza. This sub-sprint adds no new semantic
surface; per-item verification (§6) is the acceptance approach.

## 10. Bundle policy

- Dev stages ONLY S2-scope files in the dev commit (per `iteration_
  governance.md` §8.7).
- Deliver-agent files (close artefacts for next sub-sprint or
  milestone) are bundled by the human at close commit (NOT staged by
  dev).
- Dev MUST NOT `git add -A`; staged files MUST be enumerated
  explicitly.
- Dev MUST NOT touch any file under §5 (hard fences).

## 11. Handoff schema

Dev produces `docs/sprints/sprint-048-handoff.md` with these sections
(per existing S-Cleanup-1 / S-Eval-5 convention):

1. **Identity** — sub-sprint name (Sprint 48 / S-Cleanup-2), dev
   commit SHA, branch, HEAD before / after.
2. **Scope landed** — per §4 table, actual files + numstat (`git show
   --numstat <commit>`).
3. **§3 layer-classification + anti-hardcode self-walk** — re-state
   the stanza with delivered evidence.
4. **Java baseline** — must be unchanged `1163 / 1-inherited / 0 / 2`.
5. **Python baseline** — preserved or improved (no new failures from
   S2; NEW tests for Item #2 should PASS).
6. **Per-item verification evidence** — per §6 (cite grep / Python
   invocation / smoke command output).
7. **Alice strip direction verification** — document the decision
   (strip vs reverse) + cite which M3-Eval design doc + S-Eval-4
   archive cell supports the chosen direction.
8. **OQs surfaced** — questions for deliver-agent + human disposition.
9. **Drift items** — line counts vs estimate, test count vs estimate,
   etc.
10. **Hard fence honored checklist** — per §5 file-NOT-in-scope list.
11. **R-item flip request** — `R-bad-case-fixture-migrate-to-l3-judge-
    dims` → CLOSED (deliver-agent does the actual flip at close).
12. **Closure verdict deferred** — template: A — Clean PASS / B fix-
    iteration / C in-flight downgrade / out-of-scope-review.

## 12. Closure verdict (deferred to sub-sprint close)

To be appended at S-Cleanup-2 close by deliver-agent + human jointly.
Reserved per existing convention.
