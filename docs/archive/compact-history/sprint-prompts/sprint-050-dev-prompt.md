# Sprint 50 / M5 S1 — Eval Report Coherence — Dev Implementation Prompt

You are the dev agent for **Sprint 50 / S1**, the FIRST sub-sprint of **Milestone
M5 — Observability Coherence**. Goal: re-align the eval `report.html` with the
M3-Eval four-tier pyramid + the M4 `case_passed_authority` annotation, and remove
the stale pre-M3 Phase-5 §6.9 dashboard. **Display + docs-only. Zero `server/`
touch. Zero scoring change. Zero fixture change.** Read this prompt + the
contracts in §1 before writing any code.

This sub-sprint consumes the OPEN R-item `R-eval-report-observability`. The human
chose **Alternative B** (remove the legacy dashboard + a foundational fold-back),
**not** the proposal's recommended Alternative A — see §2 for exactly how that
changes the build.

## 1. Read order (cold start)

1. `AGENTS.md` (auto-loaded constitution chain). Do not re-read if in context.
2. `docs/solutions/eval_report_coherence_proposal.md` — **READ FIRST AND IN
   FULL.** It is the detail design: §2 current-state survey (exact functions +
   line numbers), §5 **Alternative A** (the four-tier layout mockup — build this
   surface), §9 hard fences, §11 `json_report.py` mirror note. **Caveat: the
   proposal recommends Alternative A; we are doing Alternative B** (§5 Alt B) —
   see §2 of this prompt for the delta.
3. `docs/sprint_objective.md` — the Sprint 50 contract (primary; the numbered
   scope #1-#6 is binding).
4. `docs/milestone_objective.md` §3 S1 + §6 hard fences (milestone-level).
5. `docs/10-handoff.md` §0 — baselines (`3 failed, 460 passed` Python via
   `uv run python -m pytest`; Java untouched this sub-sprint).

## 2. What "Alternative B" means for the build (READ CAREFULLY)

Build the four-tier verdict surface exactly as drawn in the proposal §5
Alternative A mockup — **except** the human chose Alternative B, so:

- **DO** build the four sections + the `suite_authority` header + per-case
  tier-labelled badges (the proposal §5 Alt A layout, minus its last box).
- **DO NOT** keep the "Phase-5 trend metrics (legacy; informational only)"
  section. Per Alt B, the Phase-5 §6.9 7-metric dashboard *rendering* is
  **removed entirely** from the HTML.
- **DO** additionally perform the §6.9 foundational fold-back (scope #4) — Alt A
  did not require this; Alt B does.

So: Alt B = (Alt A's four-tier surface) − (the legacy dashboard render) + (the
§6.9 supersession fold-back).

## 3. Items

### #1 — Four-tier HTML verdict surface (`report/html_report.py`)
Per `docs/sprint_objective.md` #1. Header block (`run_id`, label,
`suite_authority` badge + "programmatic counts are informational for
human-judgment suites" note); then four sections:
- **Tier-0 Safety Floor** — the existing L1 hard checks, labelled Tier-0.
  *(Display nuance, optional: the 2 `no_pii_leakage` regex hits on cs011/cs012
  are known false-positives confirmed at M4 close — you MAY render an
  informational sub-annotation, but do NOT add a filter on the underlying check;
  it is informational rendering only. Proposal §10 Risk 2.)*
- **Tier-1 Outcome** (PRIMARY for human-judgment suites) — show
  `suite_authority`, the informational programmatic `case_passed` count, and a
  pointer to `eval_interactive/case_specs/bad_cases/_manifest.md` for the
  human-review verdict.
- **Tier-2 Critical-flow** — **NEW `_render_tier2`** surfacing the per-case
  `tier2_result` (passed / severity / `failed_step_ids`). This field already
  exists on every case dict in `results.json` (executor populates it) but is
  **never rendered today** — that is the gap.
- **Tier-3 Polish** — advisory L2/L3 dims, clearly marked "never flips
  case_passed."
- Per-case: replace the single binary PASS/FAIL `<span class="badge">` (current
  `_render_case`, ~line 451-508) with a **tier-labelled badge**.

### #2 — Remove the Phase-5 dashboard *rendering* (`report/html_report.py`)
Remove `_metrics_dashboard` (~318-355) and the "0/N passed" headline framing in
`_pass_fail_overview` (~357-402). **The aggregates STAY computed in
`_compute_summary` and present in `results.json`** (backward-compat — display
stripped, data preserved; human-confirmed). The per-UC rollup `_per_uc_table`
(~404-437) is **kept but reframed** under the four-tier view (label it a
programmatic-suite rollup), **not deleted**.

### #3 — `suite_authority` aggregate (`batch/executor.py::_compute_summary`, ~832-925)
Compute **once**: `"human_review"` if all cases human_review, `"programmatic"` if
all programmatic, `"mixed"` if both — via the existing
`sets.is_human_judgment_suite(...)` over per-case `case_passed_authority`. ADD it
alongside existing summary fields (do not alter existing fields). Consumed by
**both** `report/html_report.py` AND `report/json_report.py` (single source of
truth; the two renderers must not drift — proposal §11).

### #4 — Foundational fold-back (`docs/foundational/phase5_evaluation_design.md` §6.9, ~line 1146)
Mark §6.9 (the "Top-Line Metrics 7 Key" dashboard spec) **superseded by the
M3-Eval four-tier pyramid**, with a pointer to `docs/milestones/M3-Eval_objective.md`
+ `docs/current/iteration_governance.md` §5.5/§5.6. **Per `doc_governance.md`:
PRESERVE the §6.9 content (do NOT delete it), add a clearly-marked supersession
note, set the relevant front-matter `implementation_status` / a §6.9 status
annotation, bump `last_reviewed`. Do NOT rewrite any other section of phase5.**
This is the ONE foundational doc edit; it is a stated intentional fold-back (not a
silent sprint-by-sprint rewrite). Flag it explicitly in the handoff for Codex.

### #5 — Re-render regression check
Regenerate `eval_interactive/results/20260523-075141/report.html` from the
existing `results.json` with the new renderer. Confirm the four-tier surface
displays and the misleading "0/12 passed" headline is gone. **Do NOT regenerate
`results.json`.**

### #6 — Tests (`eval_interactive/tests/test_report_generators.py`)
Extend with: (a) four-tier render assertions; (b) `suite_authority` resolution
(human_review / programmatic / mixed); (c) `_render_tier2` surfaces a
`tier2_result`; (d) a regression asserting the removed Phase-5 dashboard no longer
renders.

## 4. Hard fences (from `docs/sprint_objective.md` §"Hard fences" — DO NOT VIOLATE)

- No `composite.py::compute_composite` / scoring-weight / `>= 0.7` threshold
  change.
- Do NOT couple the Tier-2 HTML display to the §5.6 human-judgment gate.
- No `_resolve_case_passed_authority` / `_OPT_IN_SETS` / `is_human_judgment_suite`
  semantics change.
- No bad-case (or any) fixture YAML edit under `eval_interactive/case_specs/**`.
- Do NOT alter `_compute_summary`'s existing `passed_cases` / `task_success_rate`
  *computation* — ADD `suite_authority`; remove only the HTML *rendering*.
- **No `server/**` touch.** No `eval_interactive/case_specs/shadow/` reads.
- Do NOT regenerate historical `results.json` files.
- Do NOT delete `phase5_evaluation_design.md §6.9` content or rewrite other
  phase5 sections — supersession-mark only.
- Do NOT implement the three N/A Phase-5 metrics.
- No `docs/sprints/*` (except NEW `sprint-050-handoff.md`), `docs/milestones/*`,
  `docs/10-handoff.md`, `docs/action_bank.md`, `docs/milestone_objective.md`,
  `docs/sprint_objective.md`, `docs/codex-findings.md` edits.
- **STOP and surface to deliver-agent** if removing the dashboard or the §6.9
  fold-back reveals a downstream consumer of the removed HTML, or if rendering the
  four-tier view appears to require a fixture/scoring change (out of scope →
  contract gap).

## 5. §4.1 anti-hardcode self-walk (sub-sprint is §7-EXEMPT)

S1 is `infra` (display) + a docs fold-back — **§7-exempt, no semantic surface.**
Still walk the §4.1 9-question kernel and capture in handoff §3; **expected: clean
`approve`** — name the exemption (display + docs-only). Key points: Q1/Q3 — no
keyword/regex/enum/per-UC matrix; `suite_authority` derives from the existing
`is_human_judgment_suite` helper. Q4 — no eval-case text / CaseSpec id encoded.
Q5/Q7 — no semantic ownership shift, tool schema / PII / grounding floors
untouched.

## 6. Tests to run

- **Python (item-specific)**: `cd eval_interactive && uv run python -m pytest
  tests/test_report_generators.py -v` — all PASS.
- **Python (full suite)**: `cd eval_interactive && uv run python -m pytest --tb=no
  -q` — **no NEW failures** vs the `3 failed, 460 passed` baseline (the 3 residual
  are env-specific per OQ-S47.3; do not fix them here). **Use `python -m pytest`,
  NOT the `pytest` console script** (it segfaults on a stale venv shebang).
- **Re-render** `results/20260523-075141/report.html` (item #5) and eyeball it.
- **No Java run** (zero `server/` touch). **No real-LLM rerun** (no bot-behaviour
  or judge change — S1 is display + docs).

## 7. Handoff + bundle

- Produce `docs/sprints/sprint-050-handoff.md` per `docs/sprint_objective.md`
  §"Handoff requirements". **§12 reserved for deliver-agent + human** (leave empty).
- Record: `git show --numstat` for the commit; the phase5 §6.9 fold-back diff; the
  re-rendered report path; the test delta; the §4.1 self-walk.
- Numbers-cite discipline: every numstat / count from a reproducible command
  (`git show --numstat`, `pytest`).
- **Stage ONLY S1-scope files**; **no `git add -A`**. Enumerate in the commit
  message. Scope files: `eval_interactive/eval_interactive/report/*` (html +
  json), `eval_interactive/eval_interactive/batch/executor.py`,
  `eval_interactive/tests/test_report_generators.py`,
  `docs/foundational/phase5_evaluation_design.md`, the re-rendered
  `eval_interactive/results/20260523-075141/report.html`, and NEW
  `docs/sprints/sprint-050-handoff.md`. Deliver-agent close-bundle files
  (milestone_objective, sprint_objective archive, action_bank, 10-handoff) are
  bundled by the human at close — do not stage them.

## 8. Self-check (before claiming complete)

- [ ] Read `eval_report_coherence_proposal.md` in full first, and understood the
  Alt A → Alt B delta (§2 of this prompt)?
- [ ] Four-tier surface built (Tier-0/1/2/3 + `suite_authority` header + per-case
  tier badges + NEW `_render_tier2`)?
- [ ] Phase-5 dashboard *rendering* removed; aggregates KEPT in `results.json`;
  per-UC rollup kept + reframed (not deleted)?
- [ ] `suite_authority` computed once in `_compute_summary`; consumed by BOTH
  html + json renderers (no drift)?
- [ ] phase5 §6.9 fold-back: content PRESERVED, marked superseded + pointer,
  `last_reviewed` bumped, no other phase5 section rewritten; flagged for Codex?
- [ ] M4 report re-rendered; "0/12" headline gone; `results.json` NOT regenerated?
- [ ] Tests: four-tier + suite_authority + `_render_tier2` + dashboard-removed
  regression PASS; full Python suite no new failures?
- [ ] §4.1 self-walk clean `approve` (display + docs-only exemption named)?
- [ ] Hard fences honored (no `server/`, no scoring, no fixture, no historical
  `results.json` regen)?
- [ ] Handoff §1-§11 complete; §12 reserved; numbers reproducible; staged only
  S1-scope files?

If any checkbox is unchecked, surface to deliver-agent BEFORE declaring complete —
do not silently ship partial work.
