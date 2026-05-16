## Sprint 29 Review Decision
decision: fix_required
blocking_count: 3
summary: Sprint 29's substantive probe shape is mostly sound (4 CaseSpecs load, Track A used real-LLM smoke evidence, Track B records the required downgrade and names but does not open the follow-on R-item, and the line-450 R-item remains open). Closure is blocked because the current review package still contains two non-packaging files outside the Sprint 29 hard fence, including a prompt edit, and the handoff leaves several quantitative claims without the required source-path plus extraction-command citation.

## Blocking Findings

1. BLOCKING - hard-fence prompt edit in the current review diff (`server/src/main/resources/prompts/system_prompt.txt:60`). The file is modified (the `ACTIVE-UC TIEBREAKER` heading changed), but Sprint 29's hard fence forbids any `system_prompt.txt` edit. This violates review prompt section 3.1 and sprint objective section 9 hard fence #3. The handoff says this is pre-existing; if so, exclude it from the Sprint 29 closure package before review/commit.

2. BLOCKING - non-packaging scope expansion in the current review diff (`csagent_system_design_review.md:85`). This file is outside Sprint 29's in-scope paths and outside the packaging-rollforward exception. This violates review prompt section 3.8. The handoff labels it pre-existing; if so, exclude it from the Sprint 29 closure package before review/commit.

3. BLOCKING - reproducibility citations are incomplete for quantitative claims in the handoff (`docs/sprints/sprint-029-handoff.md:110`, `docs/sprints/sprint-029-handoff.md:189`, `docs/sprints/sprint-029-handoff.md:191`, `docs/sprints/sprint-029-handoff.md:238`). Examples include `3 964 lines`, `219 parsed rows`, `142.2 KB`, `mean_judge: 0.8000`, `escalation_correct: 25.0%`, `mean_outcome: 0.8007`, and `confidence 0.90` without a colocated source path plus extraction command. This violates review prompt section 3.4, which makes unsourced numbers in sections 3/5/6/7 blocking.

## Non-Blocking Scope Notes

- Packaging-rollforward files are not counted as blockers: `docs/sprint_objective.md`, `docs/sprints/sprint-029-objective.md`, `docs/10-handoff.md`, `compact/sprint-029-dev-prompt.md`, `compact/sprint-029-review-prompt.md`, `compact/sprint-030-dev-prompt.md`, and `compact/sprint-030-review-prompt.md` appear to be deliver-agent close/prep packaging rather than Sprint 29 substantive scope.
- Ignored smoke artifacts at `eval_interactive/results/20260514-225419/` are present locally and support the handoff's cited jq checks, but they are not visible in normal `git status` because the results tree is ignored.

## Anti-Hardcode Kernel Verdict

Per the iteration-governance section 4.1 kernel, the Sprint 29 substantive diff does not introduce a semantic hardcode: no runtime keyword/regex/if-else/per-UC matrix was added for a semantic decision, no Tier-0 invariant was added, no prompt if-else block was added by the Sprint 29 CaseSpec work, and no CaseSpec id was mirrored into runtime/prompt/judge code. The Sprint 29 Q8 deviation is acceptable for this probe (target coverage across D485.2/D564.7/D616.2; neighbor/negative optional; shadow not required). The per-PR anti-hardcode verdict for the substantive Sprint 29 work is `approve`, but sprint-close remains `fix_required` because of the blocking packaging/reproducibility issues above.

## Verification Notes

- CaseSpec loader check: `cd eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_specs; from pathlib import Path; print(len(load_case_specs(Path('case_specs/case_families/sprint29_directive_probe'))))"` returned `4`.
- Results summary check: `jq '{n_cases:(.case_results|length), total_llm_calls:([.case_results[].llm_calls|length]|add), per_turn_phase_counts:([.case_results[].per_turn_trace[] | (.phase_plan.phase // "NULL")] | group_by(.) | map({phase:.[0],count:length}))}' eval_interactive/results/20260514-225419/results.json` returned 4 cases, 40 LLM calls, and 5 `RESOLVE` phase-plan turns.
- R-item discipline check: `docs/action_bank.md:450` is updated, remains open, and no table row opens `R-per-turn-phase-transition-dump-for-smoke-harness`.
- Third defensive shape check: `jq -r '.case_results[] | select(.case_id=="cs_interactive_029") | .per_turn_trace[] | "phase=" + (.phase_plan.phase // "phase unknown")' eval_interactive/results/20260514-181257/results.json` returns `phase=DISCOVER` and `phase=phase unknown`, so the review extraction path does not throw on NULL `phase_plan`.
