---
title: Sprint objective — Sprint 50 / M5 S1 — Eval Report Coherence
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-24
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-049-objective.md]
superseded_by: null
notes: >
  Approved by human 2026-05-24. FIRST sub-sprint of Milestone M5 —
  Observability Coherence. Consumes R-eval-report-observability. Human chose
  Alternative B (remove the legacy Phase-5 §6.9 dashboard rendering + a
  foundational fold-back marking §6.9 superseded) 2026-05-24, over the
  proposal's recommended Alternative A. Display + docs-only; zero server; zero
  scoring; zero fixture. §7-exempt (no semantic decision). Detail proposal:
  docs/solutions/eval_report_coherence_proposal.md.
---

# Sprint 50 / M5 S1 — Eval Report Coherence

## Class

`infra` (eval-harness report rendering) + a single intentional **foundational
docs fold-back** (`phase5_evaluation_design.md §6.9`). **§7-exempt**: display +
docs-only; no prompt, runtime semantic decision, eval-spec scoring, or judge
calibration change. (A short anti-hardcode stanza is included at §A below for
Codex clarity even though the sub-sprint is exempt.)

## Goal

`report.html` becomes the **M3-Eval four-tier verdict surface** with a prominent
`suite_authority` badge, and the **pre-M3 Phase-5 §6.9 7-metric dashboard is
removed from the HTML** (its computed aggregates remain in `results.json` for
backward-compat — display stripped, data preserved). `phase5_evaluation_design.md
§6.9` is marked superseded by the four-tier pyramid via a minimal fold-back. A
human opening the report on the bad-case (human-judgment) suite sees "this is a
human-judgment suite — see the manifest for the verdict," not a misleading "0/12
passed."

## Scope (numbered; this is the contract)

**#1 — Build the four-tier HTML verdict surface** in
`eval_interactive/eval_interactive/report/html_report.py`:
- A header block: `run_id`, label, and a `suite_authority` badge
  (HUMAN-JUDGMENT vs PROGRAMMATIC) with a one-line "programmatic counts are
  informational, not the gate, for human-judgment suites" note.
- Four sections — **Tier-0 Safety Floor** (the L1 hard checks, labelled as
  Tier-0), **Tier-1 Outcome** (the PRIMARY gate for human-judgment suites; show
  `suite_authority`, the informational programmatic `case_passed` count, and a
  pointer to `eval_interactive/case_specs/bad_cases/_manifest.md` for the
  human-review verdict), **Tier-2 Critical-flow** (NEW `_render_tier2` surfacing
  `tier2_result` — passed / severity / `failed_step_ids` — currently never
  rendered), **Tier-3 Polish** (advisory; the demoted L2/L3 dims, clearly marked
  "never flips case_passed").
- Per-case: replace the single binary PASS/FAIL badge with a **tier-labelled
  badge** (e.g. `Tier-0: PASS · Tier-1: HUMAN_REVIEW · Tier-2: 1 critical fail`).

**#2 — Remove the Phase-5 §6.9 7-metric dashboard *rendering*** from
`html_report.py` (`_metrics_dashboard` and the "0/N passed" headline framing in
`_pass_fail_overview`). The corresponding aggregates **stay computed** in
`_compute_summary` and present in `results.json` (backward-compat for downstream
consumers); **only the HTML rendering is removed** (display-only removal,
human-confirmed 2026-05-24). The per-UC rollup (`_per_uc_table`) is **kept but
reframed** under the four-tier view (labelled as a programmatic-suite rollup),
not deleted.

**#3 — `suite_authority` aggregate flag**, computed **once** in
`eval_interactive/eval_interactive/batch/executor.py::_compute_summary`:
`"human_review"` if all cases are human_review, `"programmatic"` if all
programmatic, `"mixed"` if both — via the existing
`sets.is_human_judgment_suite(...)` helper over per-case `case_passed_authority`.
Consumed by **both** `html_report.py` and `json_report.py` (single source of
truth; the two renderers must not drift). Leave all existing `_compute_summary`
fields intact — **ADD only**.

**#4 — Foundational fold-back** on `docs/foundational/phase5_evaluation_design.md
§6.9`: add a clearly-marked supersession note ("the §6.9 7-metric Top-Line
dashboard is superseded by the M3-Eval four-tier pyramid — see
`docs/milestones/M3-Eval_objective.md` + `iteration_governance.md` §5.5/§5.6")
and set the section/front-matter `implementation_status` accordingly. **Per
`doc_governance.md`: preserve the §6.9 content (do NOT delete it), mark it
superseded with a pointer, bump `last_reviewed`, and do NOT rewrite any other
section of phase5.** This is the ONE foundational doc edit and it is a stated
intentional fold-back.

**#5 — Re-render regression sanity check**: regenerate
`eval_interactive/results/20260523-075141/report.html` from the existing
`results.json` with the new renderer; confirm the four-tier surface displays and
the "0/12" headline is gone. **Do NOT regenerate `results.json`** (the underlying
data is correct; only the display layer changes).

**#6 — Tests**: extend `eval_interactive/tests/test_report_generators.py` with
(a) four-tier render assertions, (b) `suite_authority` resolution
(human_review / programmatic / mixed), (c) `_render_tier2` surfacing a
`tier2_result`, (d) a regression that the removed dashboard no longer renders.

## Hard fences / STOP conditions (do NOT do)

- Do **not** change `composite.py::compute_composite`, the
  `0.5*outcome + 0.5*judge` weighting, or the `>= 0.7` summary threshold.
- Do **not** couple the Tier-2 HTML display to the §5.6 human-judgment gate — the
  bad-case gate stays human-review-of-trace regardless of rendering.
- Do **not** change `_resolve_case_passed_authority` semantics or `_OPT_IN_SETS`
  membership (S-Cleanup-2 settled).
- Do **not** change any bad-case fixture YAML (empty `outcome_checks` /
  `llm_judge_dimensions` lists stay; S-Cleanup-2 settled).
- Do **not** alter `_compute_summary`'s existing `passed_cases` /
  `task_success_rate` *computation* — only ADD `suite_authority` and remove the
  HTML *rendering* of the dashboard.
- Do **not** touch `server/` Java (zero bot-side change).
- Do **not** regenerate historical `results.json` files.
- Do **not** delete `phase5_evaluation_design.md §6.9` content or rewrite other
  phase5 sections — supersession-mark only.
- Do **not** implement the three N/A Phase-5 metrics
  (`correct_tool_invocation_rate` / `escalation_correctness_rate` /
  `grounded_final_answer_rate`).
- **STOP and surface to deliver-agent** if removing the dashboard rendering or
  marking §6.9 superseded reveals a downstream consumer of the removed HTML
  (e.g. a doc/runbook that links a specific dashboard element), or if a bad-case
  fixture / scoring change appears necessary to render the four-tier view — that
  is out of scope and signals a contract gap.

## Test / eval requirements

- `cd eval_interactive && uv run python -m pytest tests/test_report_generators.py`
  (the new + existing cases) — green.
- Full Python suite: `uv run python -m pytest` — **no NEW regression** vs the
  `3 failed, 460 passed` baseline (the 3 residual env-specific failures are
  pre-existing; do not fix them here).
- Re-render `results/20260523-075141/report.html` (scope #5) and eyeball it.
- **No Java run required** (zero `server/` touch). **No real-LLM rerun required**
  (no bot-behaviour or judge change; S1 is display + docs).

## Handoff requirements

- Author `docs/sprints/sprint-050-handoff.md`. Leave **§12 (deliver-agent + human
  close verdict)** empty.
- Record: the `git show --numstat` for the commit; the phase5 §6.9 fold-back diff;
  the re-rendered report path; the test delta; an §A self-walk of the §7-exemption
  + the anti-hardcode stanza (Q1-Q9 trivially `approve`, display/docs-only).

## Commit discipline

Dev stages **only S1 scope**: `eval_interactive/eval_interactive/report/*`,
`eval_interactive/eval_interactive/batch/executor.py`,
`eval_interactive/tests/test_report_generators.py`,
`docs/foundational/phase5_evaluation_design.md`, the re-rendered
`results/20260523-075141/report.html`, and `docs/sprints/sprint-050-handoff.md`.
**Do NOT `git add -A`** — deliver-agent close-bundle files (milestone_objective,
this objective's archive, action_bank, 10-handoff) are bundled by the human at
close, not by the dev session.

## §A — Anti-hardcode stanza (sub-sprint is §7-exempt; included for Codex)

**Target failure layer:** `infra` (eval-harness report-rendering layer) + a
foundational docs fold-back.

**Tier-0 invariant:** adds no Tier-0 invariant. The Tier-0 safety floor is
unchanged; S1 changes only how Tier-0/1/2/3 results are *displayed* and marks a
superseded foundational dashboard spec — not which checks run or how they flip
`case_passed`.

**Semantic hardcode:** none. All new logic is display-only — a `suite_authority`
flag from existing `case_passed_authority` via the existing
`is_human_judgment_suite` helper (no new keyword/regex/enum), tier-labelled
badges from existing `tier2_result` + l1/l2/l3 structures, and removal of the
legacy dashboard rendering. Zero `composite.py` / `hard_checks.py` /
`outcome_checks.py` / `llm_judge.py` / `skill_procedure_check.py` change. Zero
`server/` touch.

**Generalization coverage:** target / neighbor / negative / shadow = 12 / 12 / 0
/ 0. Target = the M4 close run `results/20260523-075141` (12 bad cases, all
human_review). Neighbor = the same shape under a programmatic-gate suite (an
anchor batch still renders correctly + reports `suite_authority: programmatic`).
No semantic decision in scope → negative / shadow N/A (the gate touched is visual
presentation, not bot behaviour or judge calibration).

## OQ (open questions — filled during the sub-sprint)

- _none yet_
