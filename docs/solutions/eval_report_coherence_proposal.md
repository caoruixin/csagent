---
title: Eval report coherence — re-align report.html with the M3-Eval four-tier pyramid + M4-Eval-Cleanup case_passed_authority
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-05-24
mode: bad-case-driven
---

## 1. Executive summary

The HTML report produced by `eval_interactive` is **stale relative to the M3-Eval four-tier evaluation pyramid and the M4-Eval-Cleanup `case_passed_authority` annotation**. `results.json` already carries the new fields (`case_passed_authority`, `tier2_result`, demoted-to-Tier-3 advisories) but the report renderer (`eval_interactive/eval_interactive/report/html_report.py`) and the summary aggregator (`batch/executor.py::_compute_summary`) both pre-date M3-Eval and surface only the original Phase-5 §6.9 7-metric dashboard plus a single binary `case_passed` badge.

The result is the contradiction the human observed in `eval_interactive/results/20260523-075141/report.html`:

- Dashboard header: "Passed: 0 / Failed: 12; Task Success Rate 0.0%; Mean Composite 0.21" — implies the bad-case suite catastrophically failed.
- Per-case badges (same page): 5 PASS / 7 FAIL — programmatic L1 ∧ mandatory-L2 ∧ no-Tier-2-critical-fail.
- The M4 close manual review (`eval_interactive/case_specs/bad_cases/_manifest.md` "M4-Eval-Cleanup close" section): **PASS × 5 / IMPROVING × 4 / FAIL × 3 / OOSR × 0** — distribution IDENTICAL to M3 close; regression-safety bar MET.

None of these three signals contradict each other on the underlying data; they answer different questions. The report does not say which question it is answering, does not surface that the suite is `case_passed_authority="human_review"`, does not show Tier-2 results at all, and does not distinguish Tier-3 advisories from gating checks. To a human opening `report.html` cold (as the user did), it looks like the post-M3/M4 system regressed catastrophically when in fact the milestone closed A — Clean PASS.

This is **not a copy** and **not a backwards-compatibility shim**. It is **debt deferred to a future observability milestone**, already tracked as the OPEN R-item `R-eval-report-observability` (action_bank.md:779, opened S-Cleanup-2 close 2026-05-23). The renderer has not been touched since before M3-Eval; M3-Eval and M4-Eval-Cleanup intentionally scoped their changes to schema/executor/scoring/fixture layers and explicitly excluded report rendering ("display-only; no scoring change" — R-item text).

Recommendation: schedule a small (1–2 sub-sprint) **Eval Report Coherence** sub-sprint that consumes `R-eval-report-observability` in one pass. Display-only, zero scoring change, zero `server/` touch. Path-A "additive + clearly labelled" rendering is the recommended option — the OLD 7-metric dashboard stays accessible as a "Phase-5 legacy view" but the new pyramid view becomes the primary surface, with `suite_authority` clearly badged at the top. Path-B "delete legacy view" is cleaner long-term but loses Phase-5 trend continuity on the 159-case anchor suite; not recommended.

---

## 2. Current-state survey (code-grounded)

All references verified at HEAD `d5b1508` (M4-Eval-Cleanup close commit) plus the unstaged research-agent doc-only changes; no `server/` or scoring code is in flight.

### 2.1 What `results.json` carries (post-M3 + post-M4)

Per-case dict (`eval_interactive/eval_interactive/batch/executor.py:594-797`):

- `case_passed: bool` — `compute_composite` result: `all(L1) AND all(mandatory_L2) AND not tier2_critical_failed` (`scoring/composite.py:205`).
- `composite_score: float` — `0.0 if not case_passed else 0.5 * outcome + 0.5 * judge` (`composite.py:160-161`).
- `case_passed_authority: "human_review" | "programmatic"` — NEW (S-Cleanup-2 / Sprint 48; `executor.py:43-65, 596, 699, 743, 792`). Resolved from `case_spec.source_suite` via `is_human_judgment_suite(...)` against `_OPT_IN_SETS = ("bad_cases", "anchor_outcome")` (`batch/sets.py:24`).
- `l1_results`, `l2_results`, `l3_results` — per-check lists.
- `tier2_result: Tier2Result` — NEW (S-Eval-2 / Sprint 43; populated by `compute_tier2_result` over `Skill.critical_steps` with per-step `desc` + `trace_check` DSL; phase-plan-scoped in S-Cleanup-3 / Sprint 49 #9).
- `outcome_score`, `judge_score`, `failure_tags`, `stall_detected`, `containment_outcome`, `active_use_case`, `escalation_reason`, `transcript`, `per_turn_trace`, `llm_calls`, `contract_warnings`.

For the M4 run under scrutiny: every one of the 12 cases is `case_passed_authority: "human_review"` (the suite is `bad_cases`, an opt-in human-judgment suite per §5.6).

### 2.2 What `_compute_summary` does (suite-blind aggregation)

`eval_interactive/eval_interactive/batch/executor.py:832-925` (verified):

```python
passed = sum(
    1 for r in case_results
    if r.get("case_passed") and r.get("composite_score", 0) >= 0.7
)
failed = total - passed
```

- The aggregator **does not branch on `case_passed_authority`**. It applies the programmatic gate (`case_passed AND composite >= 0.7`) uniformly across all cases, including human-judgment opt-in suites.
- For the M4 run: every bad case has `judge_score = 0.0` (no L3 dimensions configured on bad-case fixtures; the M3-Eval S-Eval-4 fixture schema convergence stripped `outcome_checks` + `llm_judge_dimensions` lists). So `composite = 0.5 * outcome + 0.5 * 0 = 0.5 * outcome ≤ 0.5`. Every case is below the 0.7 summary threshold by construction, even cases whose `case_passed=True`.
- Result: `passed_cases: 0 / failed_cases: 12 / task_success_rate: 0.0%` on a suite where the per-case `case_passed` badge says 5 PASS.

This is the source of the headline contradiction. It is not a bug in any single sprint's work — the M3-Eval composite weighting (`0.5 * outcome + 0.5 * judge`) plus the S-Eval-4 fixture schema convergence (empty L3 lists on bad cases) plus the inherited Phase-5 summary threshold (`>= 0.7`) compound to a deterministic "every bad case is below the summary pass threshold" outcome. The architecture is correct; the headline metric just no longer means what it appears to mean for human-judgment suites.

### 2.3 What `report.html` renders (Phase-5 §6.9 only)

`eval_interactive/eval_interactive/report/html_report.py` (verified):

- Docstring line 7: `"2. Summary dashboard with the 7 key metrics from Phase 5 section 6.9"` — the renderer is explicitly built to the Phase-5 spec. `docs/foundational/phase5_evaluation_design.md:1146` confirms §6.9 is the pre-M3 "Top-Line Metrics (7 Key — Interactive + Replay)" table.
- `_metrics_dashboard` (line 318-355): renders Task Success Rate / Stall Rate / Correct Tool Invocation Rate / Escalation Correctness Rate / Grounded Final Answer Rate / Policy Compliance Rate / Turns to Resolution. Three are `N/A` on the M4 run because the executor never populates `correct_tool_invocation_rate` / `escalation_correctness_rate` / `grounded_final_answer_rate` (`_compute_summary` emits `escalation_correctness` not `escalation_correctness_rate`; the other two are unimplemented).
- `_pass_fail_overview` (line 357-402): consumes `summary['passed_cases']` / `failed_cases` directly. Shows the "0/12 Passed" headline.
- `_per_uc_table` (line 404-437): per-UC pass/fail rollup using the same programmatic gate.
- `_render_case` (line 451-508): emits a single binary PASS/FAIL `<span class="badge">` from `cr.get("case_passed", False)`. No `case_passed_authority` annotation. No suite-type prefix. No "human review required" badge.
- `_render_l1` / `_render_l2` / `_render_l3` (line 510-585): renders the per-tier check tables. **There is no `_render_tier2`.** The `tier2_result` field exists on every case dict in `results.json` and is read by `_compute_summary` indirectly (via `failure_tags`), but never surfaced in HTML.
- Tier labels: the headers say "L1 Hard Checks" / "L2 Outcome Scores" / "L3 LLM Judge Scores". These pre-date the Tier-0 / Tier-1 / Tier-2 / Tier-3 pyramid naming. There is no visual indication of which L2/L3 dimensions are now Tier-3 advisory (`handover_completeness`, `case_id_present`, `relevance`, `tone_appropriateness`, `groundedness`, `tool_sequence_match`) vs Tier-1 supplementary (`user_goal_achievement`).

### 2.4 What the post-M3/M4 framework says it is

From `docs/milestones/M3-Eval_objective.md:105-110`:

- **Tier-0** (unchanged hard gate): PII / safety / identity / forbidden-phrase / escalation-compliance / phase-transition-validity. Sourced from `runtime_freeze_and_risk_policy.md` §1/§2.
- **Tier-1 Outcome** (NEW mandatory): bad-case suite (human-judgment per §5.6) + `anchor_outcome` outcome-only suite + L3 `user_goal_achievement` supplementary.
- **Tier-2 Critical-flow** (NEW): per-UC process correctness derived from `Skill.critical_steps` (`desc` + `trace_check` DSL).
- **Tier-3 Polish** (advisory only): everything else (`turn_efficiency`, `tool_sequence_match`, `issue_preservation`, `handover_completeness`, `case_id_present`, L3 `relevance`/`tone_appropriateness`/`groundedness`, latency, token cost, stall rate). Recorded for trend; never flips `case_passed`.

From `iteration_governance.md §5.6` (governance authority):

- The bad-case suite is the **PRIMARY human-judgment gate**, not a programmatic gate. `closure_criterion` is GUIDANCE for the human reading the trace, not a programmatic match.
- "Treating a programmatic `composite_score >= X` derived from the bad-case trace as a binary gate would re-import the same instability the smoke composite_score demotion (§5.5) was designed to escape."

The headline "Task Success Rate 0.0%" on the bad-case suite report is precisely the artefact §5.6 warns against. The architecture has the right intent; the report renderer never caught up.

### 2.5 What the OPEN R-item already documents

`docs/action_bank.md:779` (`R-eval-report-observability`, opened S-Cleanup-2 close 2026-05-23, deferred to M4+ planning):

> Bundles three report-observability gaps:
> (a) OQ-S48.2 — `_resolve_case_passed_authority` surfaces `case_passed_authority` per-case but does NOT propagate to `RunResult.summary` as a top-level `suite_authority: human_review | programmatic | mixed` aggregate flag;
> (b) OQ-S48.3 — the HTML report template does NOT render `case_passed_authority` (JSON carries it; HTML view does not visually distinguish human_review from programmatic rows);
> (c) the original post-M3-Eval cleanup audit P2 finding "HTML report 不显示 Tier-2 / severity 信息"

The R-item explicitly enumerates two implementation paths and explicit anti-framings ("display-only; no scoring change"; "do not couple the HTML Tier-2 display to the §5.6 human-judgment gate"). It is labelled "low priority observability nicety" and was deferred because M4-Eval-Cleanup intentionally hard-fenced report-rendering work out of scope.

So the R-item knows about this. The reason the user saw the contradiction is **not** that the system forgot — it's that the R-item's "low priority" classification under-weights the human cost of opening a stale report. The first-order user impact (a human-in-the-loop reading the report cold and concluding the system regressed) is higher than "observability nicety" implies.

---

## 3. Gap analysis / Root-cause analysis (multi-layer)

This is Mode 2 / Path-2 bad-case-driven research — the bad case here is **the report itself**, not a bot trace.

### 3.1 The four-layer disconnect

| Layer | Status post-M3/M4 | Report.html shows it as |
|-------|-------------------|-------------------------|
| **Tier-0** safety floor | Per-case L1 hard checks intact (`no_pii_leakage`, `no_critical_policy_violation`, `escalation_compliance`, `phase_transition_validity`, etc.) | "L1 Hard Checks" section (correct, but unlabelled as Tier-0; the 2 false-positive `no_pii_leakage` regex hits on cs011/cs012 look like real Tier-0 violations) |
| **Tier-1** outcome (bad-case human-judgment) | `case_passed_authority="human_review"` on every bad case; `closure_criterion` is human-judgment per §5.6 | Shows binary `case_passed` programmatic badge; "Passed: 0 / Failed: 12" headline; no human-judgment indicator |
| **Tier-2** critical-flow (skill_procedure_followship) | `tier2_result` populated per case; phase-plan-scoped after S-Cleanup-3 #9 | Not rendered at all. Tier-2 failures surface only as `TIER2:*` text in the failure-tags pill list |
| **Tier-3** polish / advisory | `handover_completeness`, `case_id_present` demoted in S-Cleanup-3 #4; legacy L3 dims demoted in S-Eval-5; never flip `case_passed` | Rendered in "L2 Outcome Scores" / "L3 LLM Judge Scores" with no visual distinction from gating checks |

### 3.2 Why the headline metric is so far below the per-case badges

Three independently-correct decisions compound into a misleading summary:

1. **Composite weighting**: `composite = 0.5 * outcome + 0.5 * judge` (`composite.py:160`). Sensible when L3 is populated; degenerate to `0.5 * outcome` (≤ 0.5) when L3 is absent.
2. **S-Eval-4 fixture schema convergence + S-Cleanup-2 schema unification**: bad-case fixtures intentionally carry empty `outcome_checks` + `llm_judge_dimensions` lists. The §5.6 gate is human-judgment; per-fixture programmatic dimensions would re-import the instability §5.5 demoted.
3. **Inherited Phase-5 summary threshold**: `passed = case_passed AND composite >= 0.7` (`executor.py:858`). The 0.7 threshold was tuned for the L1+L2+L3-populated smoke/anchor regime.

So `composite ≤ 0.5 < 0.7` for every bad case by construction, regardless of how many cases the human verdict counts as PASS. Each individual decision is reasonable; the combined effect is that the summary metric is **mechanically guaranteed to read poorly on the human-judgment suite**. None of the three decisions should be reverted; the headline metric needs to **stop being applied to opt-in human-judgment suites** (or be applied with a clear "not the gate" annotation).

### 3.3 Fix Layer Classification (per `iteration_governance.md §3.2`)

This is `infra` / eval-harness report rendering. Walking §3.2:

1. Infra crash / timeout? — No.
2. Tier-0 invariant broken? — No. (The Tier-0 invariants are intact; the report just mis-displays them by lumping false-positive regex hits with real violations.)
3. LLM-projection wrong? — No (this isn't a bot-side failure).
4. Skill-state losing state? — No.
5. LLM choosing wrong action? — No.
6. CaseSpec / judge asking wrong question? — Borderline: the summary aggregator is "asking the wrong question" of the human-judgment suite, but the underlying CaseSpecs are correct. This is closer to `infra` / report-side than `eval_spec`.
7. Product/policy decision needed? — No.

**Classified: `infra` (eval-harness display layer)** — no semantic hardcode in scope; no Tier-0 candidate; no §1.7 cross.

### 3.4 Why now / why not stay deferred

The R-item was correctly deferred at S-Cleanup-2 close as low-priority observability when there was an active milestone (M4-Eval-Cleanup) that didn't touch report-rendering. The current state is different:

- **No active milestone** (per `10-handoff.md §0`: "Awaiting M5+ candidate selection").
- The user surfaced the issue cold-reading the report — a higher-evidence signal than the R-item's original "OQ-S48.2 + OQ-S48.3 + audit P2" provenance.
- The bad-case suite is the **PRIMARY** acceptance gate per §5.6, and the report is the primary artefact a deliver-agent + human consult at milestone close. A primary gate whose primary report misrepresents the verdict is a higher-priority cleanup than "observability nicety".

The R-item is correctly scoped but its priority should be elevated when M5+ planning happens.

---

## 4. Coverage check (Path-2 mandatory)

Per `iteration_governance.md` research-agent process, comparison against existing R-items + active scope:

| Existing R-item | Overlap with this proposal |
|---|---|
| `R-eval-report-observability` (action_bank.md:779) | **Direct overlap — this proposal IS the consumption of this R-item.** Bundles (a) `suite_authority` summary flag, (b) HTML render `case_passed_authority`, (c) HTML render Tier-2 / severity. All three are core to this proposal. |
| `R-bad-case-metadata-field-name-canonicalize` (action_bank.md:778) | Adjacent; the metadata field-name variance (Alice `original_session_id` vs cs001 `source_session_id`) does not affect rendering. Could optionally be bundled if the proposal touches per-case metadata display, but the metadata is human-judgment context already in YAML; orthogonal. |
| `R-case-families-manifest-cs095-smoke-vs-anchor-orphan` (action_bank.md:770) | Unrelated (case_families docs-only). |
| `R-bad-case-parallel-session-establishment-flakiness` | Unrelated (parallel session establishment is bot-side / harness session-create). |

| Active milestone scope | Overlap |
|---|---|
| None — no active milestone | n/a |

| Candidate M5+ slate (per `10-handoff.md §0` + `M4-Eval-Cleanup_objective.md` §12.10) | Overlap |
|---|---|
| M3-B Single Handover Orchestrator | None |
| UC-G/H/I/J bad-case seeding | None (would add to the suite the report under-reports) |
| Semantic-planner soft-signal extension | None |
| M3-Corpus | None |
| Latency / Skill-Tuning / Tier-0 re-evaluation | None |

No overlap conflicts. This proposal **consumes one open R-item cleanly** and does not collide with any candidate M5+ scope. It could ship as either a standalone single-sub-sprint milestone (per §8.5 "milestone of one") or be bundled with any M5+ milestone that wants the report observability shipped alongside its substantive work.

---

## 5. Design alternatives

### Alternative A — Additive render + clearly-labelled legacy view (RECOMMENDED)

The HTML report grows a new **"Four-tier verdict"** section at the top, ABOVE the existing Phase-5 §6.9 dashboard. The Phase-5 dashboard stays but is re-labelled "Phase-5 trend metrics (informational; not the gate for human-judgment suites)" and rendered in muted styling.

New top-of-report structure:

```
┌─ Header (run_id, label, suite_authority badge) ────────────────────┐
│ Suite type: HUMAN-JUDGMENT (per §5.6) — manual review required     │
│ Programmatic counts below are informational, not the gate.          │
└────────────────────────────────────────────────────────────────────┘

┌─ Tier-0 Safety Floor ──────────────────────────────────────────────┐
│ ✓ no_pii_leakage:        10/12  (2 regex false-positives flagged)  │
│ ✓ no_critical_policy:    12/12                                     │
│ ✓ escalation_compliance: 11/12                                     │
│ ✓ phase_transition:      12/12                                     │
└────────────────────────────────────────────────────────────────────┘

┌─ Tier-1 Outcome (PRIMARY GATE for human-judgment suites) ──────────┐
│ Suite authority: human_review (12/12 cases)                        │
│ Programmatic case_passed: 5/12 (informational)                     │
│ → Human review of per-case closure_criterion REQUIRED for verdict. │
│ See `eval_interactive/case_specs/bad_cases/_manifest.md` for the   │
│ deliver-agent + human verdicts from the milestone close.            │
└────────────────────────────────────────────────────────────────────┘

┌─ Tier-2 Critical-flow (skill_procedure_followship) ────────────────┐
│ Cases with Tier-2 failures: 4/12                                   │
│ - cs066: uc-k-intake-complete-before-handover (advisory)           │
│ - cs012: uc-h-intake-complete-before-handover (critical)           │
│ - cs095: search-knowledge-before-faq-answer (critical)             │
│ - wmkb:  search-knowledge-before-faq-answer (critical)             │
└────────────────────────────────────────────────────────────────────┘

┌─ Tier-3 Polish (advisory only — never flips case_passed) ──────────┐
│ Mean outcome (L2 advisory): 0.71  Mean judge (L3 advisory): 0.0     │
│ ... [details collapsible] ...                                      │
└────────────────────────────────────────────────────────────────────┘

┌─ Phase-5 trend metrics (legacy; informational only) ───────────────┐
│ [the existing 7-card dashboard, with a small label header          │
│  "Phase-5 legacy view — see §6.9 of phase5_evaluation_design.md;   │
│   not the primary gate post-M3-Eval"]                              │
└────────────────────────────────────────────────────────────────────┘

[Per-case details — each case gets a tier-labelled badge: e.g.,     ]
[ "Tier-0: PASS  Tier-1: HUMAN_REVIEW  Tier-2: 1 critical failure"  ]
[ instead of the single binary "PASS / FAIL" badge.                  ]
```

**Pros**:
- Solves the headline contradiction immediately — human opens the report and sees "this is human-judgment; check manifest for verdict" rather than "0/12 passed".
- Preserves Phase-5 trend continuity for the 159-case anchor suite (where the 7-metric dashboard still meaningfully aggregates `case_passed_authority="programmatic"` results).
- Display-only. Zero scoring code change. Zero `server/` touch. No fixture change.
- Implementable in one sub-sprint (estimate: 2-3 dev days; ≈ 250-400 LOC in `html_report.py` + ≈ 20-40 LOC in `_compute_summary` for the `suite_authority` flag + ≈ 30 LOC of tests).

**Cons**:
- The report grows in length. Mitigated by `<details>` collapsing on Tier-3 + Phase-5 sections.
- Two "verdict" surfaces (Tier-1 human-review pointer + the Phase-5 legacy view) until the legacy view can be retired. Mitigated by clear "informational only" labelling.

### Alternative B — Replace Phase-5 view entirely, with a migration note

Remove the Phase-5 §6.9 7-metric dashboard entirely. Replace with the four-tier surface only. Add a one-line note: "Phase-5 §6.9 metrics removed in favour of M3-Eval four-tier pyramid; see foundational/phase5_evaluation_design.md for archived spec."

**Pros**:
- Cleanest end-state. Single primary view.
- Forces alignment between report and governance.

**Cons**:
- Loses Phase-5 trend continuity. The 159-case anchor suite has historical reports going back many sprints that use the 7-metric framing; comparability suffers.
- Requires a foundational-doc fold-back on `phase5_evaluation_design.md` §6.9 (re-label as "superseded"), which is a separate governance task.
- The `_compute_summary` aggregator computes `escalation_correctness` / `policy_compliance_rate` / `mean_turns_to_resolution` that are still useful trend signals on the programmatic-gate suites; deleting them throws away non-trivial signal.

### Alternative C — Defer further; add only a single banner

Add only a single banner at the top of `report.html` saying "If this run is on a human-judgment suite (bad_cases / anchor_outcome), the programmatic counts below are NOT the gate — see the manifest for the human-review verdict." Defer Tier-2 rendering, per-case tier badges, and the `suite_authority` summary flag to a future cleanup.

**Pros**:
- Smallest possible change (≈ 30 LOC). Solves the immediate cold-read confusion.
- Buys time to decide between Alternative A and B properly.

**Cons**:
- Does not consume the R-item cleanly (Tier-2 rendering + `suite_authority` summary flag remain deferred).
- Leaves the per-case badges still showing binary PASS/FAIL with no authority annotation.
- High likelihood the "deferred remainder" sits in the backlog through M5/M6 without further action — the same pattern that produced the current state.

---

## 6. Recommended option + rationale

**Recommend Alternative A** (additive + clearly-labelled legacy view).

Rationale:
- Solves the cold-read problem the user surfaced. A human opening the report sees the suite type and gate type prominently before they see programmatic counts that contradict the human-judgment verdict.
- Consumes `R-eval-report-observability` cleanly in one sub-sprint (all three sub-items: `suite_authority` summary flag, per-case `case_passed_authority` rendering, Tier-2 / severity rendering).
- Preserves Phase-5 trend continuity for the programmatic anchor / smoke suites where the 7-metric view is still meaningful.
- Avoids the governance work of marking Phase-5 §6.9 superseded (Alternative B's overhead) — the fold-back can happen later on the normal cadence.
- Zero scoring change, zero `server/` touch, zero fixture change. Honours all R-item explicit anti-framings.

Alternative B is a cleaner end-state but the additional governance fold-back work + loss of historical comparability is not warranted unless a future milestone has independent reason to fold-back `phase5_evaluation_design.md`. Alternative C is a half-measure that leaves the structural problem in place.

---

## 7. Scope split + delivery priority suggestion

The deliver-agent should make the final call, but a reasonable shape:

- **As a milestone-of-one sub-sprint (per §8.5)**: "S-Report-Coherence-1" — single sub-sprint, ≈ 2-3 dev days, consumes `R-eval-report-observability`. Suitable if M5+ planning is otherwise busy with substantive bot-side work and the deliver-agent wants this off the backlog without crowding a substantive milestone.

- **OR bundled as the first sub-sprint of a "M5 Report + UC-seeding" milestone**: if M5 picks the "UC-G/H/I/J bad-case seeding" candidate (per M4-Eval-Cleanup §12.10 slate), shipping the report coherence work FIRST means the new bad cases land in a report that already correctly displays them. Strong sequencing argument for this bundle.

- **OR deferred to M6+ if M5 picks "M3-B Single Handover Orchestrator"**: the M3-B work is the P0 release-gate blocker and should not be diluted. The report-coherence work can wait — but the user should be told to read the `_manifest.md` per-case verdicts directly until then, not the report header.

**Priority recommendation**: **medium-high, not low**. The R-item's "low priority observability nicety" label under-weighted the human cost. A bad-case suite whose report misrepresents the gate it is supposed to support is a soft-but-real impediment to the §5.6 manual-review workflow. If M5 candidate selection happens within the next 2-4 weeks, prefer bundling this in as a fast-cleanup sub-sprint at the front of whichever M5 milestone is picked.

---

## 8. Layer classification + §7 stanza pre-fill draft

For when the deliver-agent drafts the sub-sprint contract:

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (eval-harness report-rendering layer)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The Tier-0
safety floor is unchanged; this sprint only changes how Tier-0 / Tier-1
/ Tier-2 / Tier-3 results are *displayed* in the HTML report, not which
checks are computed or how they flip case_passed.

**Semantic hardcode:** No semantic hardcode introduced. All new logic is
display-only: (a) a `suite_authority: human_review | programmatic |
mixed` aggregate flag computed from existing per-case
`case_passed_authority` values via the existing `is_human_judgment_suite`
helper (no new keyword / regex / enum); (b) per-case tier-labelled
badges rendered from existing `tier2_result` + `l1_results` + l2/l3
structures (no new scoring logic); (c) a "Phase-5 legacy view"
re-labelling of the existing 7-metric dashboard. Zero `composite.py` /
`hard_checks.py` / `outcome_checks.py` / `llm_judge.py` /
`skill_procedure_check.py` change. Zero `server/` touch.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: 12 / 12 / 0 / 0. The "target" is the M4 close run
`results/20260523-075141` (12 bad cases). "Neighbor" is the same data
shape under a programmatic-gate suite (run any anchor batch and verify
the Phase-5 legacy view still renders correctly). No semantic decision
in scope, so negative / shadow are N/A — the gate this sub-sprint
touches is *visual presentation*, not bot behaviour or judge
calibration.
```

---

## 9. Hard fences + non-goals

**In scope**:
- `eval_interactive/eval_interactive/report/html_report.py` — section restructure + new Tier-0/1/2/3 sections + `suite_authority` header + per-case tier-labelled badges + legacy-view re-labelling.
- `eval_interactive/eval_interactive/batch/executor.py::_compute_summary` — add `suite_authority` aggregate field (`"human_review"` if all cases are human_review; `"programmatic"` if all programmatic; `"mixed"` if both); leave existing aggregates intact.
- Tests under `eval_interactive/tests/test_report_generators.py` — extend with rendering + suite_authority cases.
- Re-render the M4 close report (`results/20260523-075141/report.html`) using the updated renderer as a regression sanity check; do NOT regenerate `results.json` (the underlying data is correct; only the display layer changes).

**Hard fences (do NOT do)**:
- Do NOT change `composite.py::compute_composite` or any scoring threshold. The `0.5 * outcome + 0.5 * judge` weighting and the `>= 0.7` summary threshold both stay. (R-item anti-framing #1.)
- Do NOT couple the HTML Tier-2 display to the §5.6 human-judgment gate. The bad-case suite gate stays human-review-of-trace per §5.6 regardless of report rendering. (R-item anti-framing #2.)
- Do NOT change `_resolve_case_passed_authority` semantics or `_OPT_IN_SETS` membership. (S-Cleanup-2 contract, settled.)
- Do NOT change any bad-case fixture YAML. The empty `outcome_checks` / `llm_judge_dimensions` lists stay. (S-Cleanup-2 settled.)
- Do NOT touch `_compute_summary`'s `passed_cases` / `task_success_rate` aggregation logic — only ADD the `suite_authority` flag alongside existing fields. (Backwards-compat for downstream consumers of `results.json`.)
- Do NOT touch `server/` Java code. (Zero bot-side change.)
- Do NOT regenerate historical `results.json` files for prior runs. The historical data is correct; old reports can be re-rendered from the existing JSON if and when needed.
- Do NOT mark `phase5_evaluation_design.md` §6.9 as superseded in this sub-sprint. (That's a separate foundational fold-back; this sub-sprint only re-labels the dashboard in the report HTML.)

**Non-goals**:
- Replacing the Phase-5 7-metric dashboard entirely (Alternative B; out of scope).
- Implementing `correct_tool_invocation_rate` / `escalation_correctness_rate` / `grounded_final_answer_rate` (the three "N/A" Phase-5 metrics — they have been N/A since pre-M3; out of scope here).
- Adding new judge dimensions or new Tier-2 critical_steps.
- Touching the `bad_case_metadata` field-name variance (`R-bad-case-metadata-field-name-canonicalize` stays a separate R-item).
- Touching the `case_families/_manifest.yaml` cs095 orphan (`R-case-families-manifest-cs095-smoke-vs-anchor-orphan` stays separate).

---

## 10. Risk + compounding-effect analysis

**Risk 1 — Tier ordering matters; if Tier-1 ships before Tier-2 rendering, the human still sees an incomplete picture.**
Mitigation: ship all three R-item bundle items in the same sub-sprint per Alternative A (do not split into "JSON-summary-flag now, HTML-rendering later" per R-item Implementation Path 2 — that's the partial state the user surfaced today). The R-item's Path 1 ("single observability pass") is the right choice for this reason.

**Risk 2 — The new "Tier-0 Safety Floor" section will visually surface the 2 `no_pii_leakage` regex false-positives (cs011 system `noreply@gumtree.com`, cs012 echoed `kitten.seller@example.com`) more prominently than today.**
Mitigation: render Tier-0 violations with a sub-annotation when the underlying detail matches known false-positive patterns (e.g., "regex flagged 2 hits; system / fixture echo confirmed via human review at M4 close — see manifest"). This is *informational rendering*, not a new filter on the underlying check. If the human prefers a stricter "always show every regex hit even if classified false-positive at milestone close", make this a config flag.

**Risk 3 — A second renderer path could drift from the first.**
Mitigation: there is currently one renderer (`html_report.py`). The S-Cleanup-2 work also added `json_report.py`. Ensure both stay in sync by having the same `suite_authority` propagation logic live once in `_compute_summary` (computed once, consumed by both report layers).

**Risk 4 — Compounding with `R-bad-case-metadata-field-name-canonicalize`.**
If the metadata-field-name R-item ships first, the renderer can rely on a unified field name (`source_session_id` everywhere). If the renderer ships first, it needs `getattr(metadata, "source_session_id", None) or getattr(metadata, "original_session_id", None)` defensive coalescing in 1-2 places. The renderer-first ordering is fine; the field-name R-item is independent.

**Risk 5 — Compounding with future Tier-0 elevation candidates (`C2` / `C3` from M2-close DEFER).**
If a future sprint elevates a new check to Tier-0, the renderer's "Tier-0 Safety Floor" section needs to pick it up. Mitigation: drive the section from a list of L1-check names tagged "tier_0" rather than hardcoding the 4-5 names. The existing `hard_checks.py` already has the structure; the renderer can reflect on `check_name` membership in a `TIER_0_CHECKS` tuple co-located with the runtime contract.

**Risk 6 — User reads this proposal and pushes for Alternative B instead of A.**
This is a legitimate human-judgment call. If the user prefers the cleaner end-state, the foundational fold-back on `phase5_evaluation_design.md` §6.9 is straightforward and could be bundled (estimate: +1 dev day for the docs work). The trade-off is loss of historical anchor-suite trend comparability.

---

## 11. Observability / trace / report implications

The proposal IS the observability work. The only further implications:

- **`json_report.py` mirror update**: ensure the JSON report includes the new `suite_authority` summary field (so external consumers of `results.json` can branch on it).
- **Per-case `tier2_result` in JSON**: already present (verified). No JSON schema change needed.
- **Historical reports re-rendering**: if and when needed, the existing `results.json` files under `eval_interactive/results/*/` carry sufficient data to regenerate the new HTML view. A small CLI helper (`eval-interactive report --from results.json --out report.html`) could be added if useful, but it is OUT OF SCOPE for this sub-sprint.
- **Documentation**: `eval_interactive/case_specs/bad_cases/_manifest.md` already documents the per-milestone-close manual review verdicts in the canonical place. The renderer should LINK to this file ("See `eval_interactive/case_specs/bad_cases/_manifest.md` for the deliver-agent + human verdicts from the last milestone close") rather than try to reconstruct verdicts inside the report — those verdicts are human-authored, not derivable from `results.json`.

---

## 12. Summary for the human

The user's intuitions are all correct:

- The framework HAS been changed to a four-tier pyramid in M3-Eval, with `case_passed_authority` annotation added in M4-Eval-Cleanup. ✓
- The report does NOT reflect those changes — it still uses the pre-M3 Phase-5 §6.9 7-metric dashboard plus a single binary PASS/FAIL badge. ✓
- This is **not a copy**, **not a backwards-compat shim**, and **not a deliberate "keep both" design**. It is **debt deferred to a future observability milestone**, already tracked as the OPEN R-item `R-eval-report-observability` (low-priority, opened S-Cleanup-2 close 2026-05-23). ✓
- The right label for the legacy view is "reference / informational, not the primary gate post-M3-Eval" — which is exactly Alternative A's proposal. ✓

The single-action ask for M5+ planning: **elevate `R-eval-report-observability` from low to medium priority and schedule it as either a milestone-of-one sub-sprint or the first sub-sprint of whichever M5 milestone is picked, especially if M5 involves further bad-case work**.

Until that lands, the source of truth for bad-case verdicts is `eval_interactive/case_specs/bad_cases/_manifest.md` (per-milestone-close manual review sections), NOT the HTML report header. The HTML per-case details are still useful for reading the trace; the dashboard numbers should be ignored for the bad-case suite.
