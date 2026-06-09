---
title: Curated bad-case suite lifecycle + smoke-demotion rationale
doc_tier: current-runtime
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Extracted from docs/current/iteration_governance.md §5.6 (incl. §5.6.1 /
  §5.6.2 / §5.6.3) and the dated rationale portions of §5.5 on 2026-06-02
  as part of the Layer A/B always-loaded split. Original section numbering
  preserved. Cite as "badcase-lifecycle §5.6.N". The §5.5 demotion RULE
  itself stays in the always-loaded iteration_governance.md; only its
  dated rationale was moved here.
---

# Curated bad-case suite lifecycle

This process doc receives the curated bad-case suite acceptance gate
and lifecycle (§5.6 and its sub-sections), plus the dated rationale
for the §5.5 smoke-composite_score demotion, moved out of the
always-loaded `iteration_governance.md` on 2026-06-02. Section numbers
are preserved against the original file so existing citations
("badcase-lifecycle §5.6.3", "§5.6") continue to resolve.

References to sections that stayed in the always-loaded Layer A read
"iteration_governance §X" (e.g., iteration_governance §5.5,
iteration_governance §5.1, iteration_governance §4) for unambiguity.

## 5.5 Smoke demotion — rationale (historical)

The smoke `composite_score` demotion rule itself stays in the
always-loaded `iteration_governance.md` §5.5. The dated rationale and
the dated specifics that motivated it are preserved here verbatim.

**Rationale:** the smoke composite_score has accumulated multiple
independent confounding sources (external LLM provider drift, judge
calibration variance, mocked-vs-real-LLM gap, unvalidated weighting
dimensions) that cannot be reliably attributed to sprint-side causes.
Per Constitution §1.6, such a metric cannot gate sprint close. See
sprint archives (Sprints 24-32) for detailed evidence history.

The dated specific trimmed from the iteration_governance §5.5
"What stays as hard close gate" list, preserved here: the Java test
suite no-new-regression bar includes the inherited
`SystemPromptUserRequestedTiebreakerTest` failure since the Sprint
24-era working-tree mod as the documented baseline.

**2026-05-21 update (Sprint 42 / M3-Eval S-Eval-1):** the outcome-only
`anchor_outcome` suite at `eval_interactive/case_specs/anchor_outcome/`
is the second human-judgment surface beside the curated bad-case suite
at `eval_interactive/case_specs/bad_cases/`; smoke remains
observation-only by design.

## 5.6 Curated bad-case suite as new primary acceptance gate

Starting 2026-05-16, the new primary acceptance gate is **manual
review of the curated bad-case suite** at
`eval_interactive/case_specs/bad_cases/`. The bad-case suite is a
deliver-agent + human curated directory of CaseSpecs derived from:

- Real user / colleague / human sessions that surfaced a multi-layer
  failure (e.g., Alice session `3772e56b-caa7-4e0a-84fc-75a26ffbe2b2`
  UC-A vs UC-H mis-classification).
- Architectural findings from sprints (e.g., Sprint 32 in-flight
  downgrade investigation findings).
- Production-readiness regression candidates the human flags as
  load-bearing for the release gate.

Each bad-case CaseSpec carries the standard CaseSpec schema PLUS:

- A `bad_case_metadata` block naming: `source_session_id` (the
  original real session that surfaced it), `surfaced_by` (the
  human / sprint that flagged it), `surfaced_date`, `failure_shape`
  (one-line description), `expected_behavior` (human-verified, NOT
  bot trace text).
- A `closure_criterion` field naming the deliver-agent + human-
  verified condition under which this bad case is considered
  "resolved" — typically expressed as observable trace evidence on
  a sprint or milestone rerun (e.g., "bot routes the topic-shift
  message to UC-C OR asks one focused clarifying question, instead
  of stamping UC-A through the drift").

**Manual review process at sprint or milestone close:**

1. Run the bad-case suite via
   `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/`
   (or the scope-relevant subset per §5.6.2 below).
2. Deliver-agent + human read the per-case traces in
   `case_results[].per_turn_trace[]`.
3. For each bad case, the human (with deliver-agent's assistance)
   judges PASS / FAIL / IMPROVING **qualitatively** against the
   `closure_criterion`. This is a **human-judgment gate**, not a
   programmatic gate — the `closure_criterion` is guidance naming
   observable end-states, but the human reads the trace and decides
   based on overall situation. Programmatic scores are unstable
   (per iteration_governance §5.5) and cannot substitute for human
   review at this stage.

**Sprint or milestone close decision** is made by the human (with
deliver-agent's recommendation) based on the per-case manual
review results PLUS the other iteration_governance §5.5 hard gates
(Codex anti-hardcode, Java tests, safety floor, grounding floor).
FAIL on a bad case does not auto-block close; it triggers a
deliver-agent + human conversation about whether the failure is
in-scope for the closing milestone or surfaces a new R-item for a
future milestone.

### 5.6.1 Bad case tiering (2026-05-17 update)

Bad cases in `eval_interactive/case_specs/bad_cases/` carry a
`tier` field in their `bad_case_metadata` block:

- **`core`** — the case is load-bearing across all milestones
  (touches a release-gate-relevant failure mode). Re-run at every
  milestone close, regardless of which milestone is closing.
- **`scope-relevant`** — the case is relevant to a specific
  architectural surface that some milestones touch and others
  don't. Re-run only at milestone closes where the closing
  milestone's `milestone_objective.md` §5 explicitly names this
  bad case in the acceptance bar.
- **`closed-as-regression-guard`** — the case has met its closure
  criterion in N ≥ 2 consecutive milestone closes (see §5.6.3
  downgrade rule). Stays in the suite; runs automatically; if
  `case_results[].terminal_outcome` returns to FAIL on a future
  run, the case auto-promotes back to active and triggers
  deliver-agent attention. No human manual review required while
  in this state unless the auto-detection fires.
- **`archived`** — the underlying failure surface has been
  structurally removed; the case can no longer manifest. Removed
  from active runs but kept in the directory as history. Requires
  deliver-agent + human joint decision documented in
  `bad_cases/_manifest.md` lifecycle ledger.

### 5.6.2 Per-milestone bad case selection

At milestone planning, the deliver-agent picks which bad cases
the closing milestone is expected to address:

- `core` cases: always run at the close (no opt-out at milestone
  planning).
- `scope-relevant` cases: named in `milestone_objective.md` §5
  acceptance bar if the milestone's scope touches the relevant
  surface. The deliver-agent SHALL list the named cases verbatim
  in the milestone objective.
- `closed-as-regression-guard` cases: run automatically; no
  scope decision required.

A bad case the closing milestone does NOT touch is NOT re-run at
that close (saves manual review time). The deliver-agent + human
revisit at the next planning round.

### 5.6.3 Bad case lifecycle downgrade (closed-as-regression-guard)

A bad case downgrades from `active` (or `scope-relevant`) to
`closed-as-regression-guard` when:

- The case has been judged PASS (per §5.6 manual review) by the
  human in **N ≥ 2 consecutive milestone closes** (deliver-agent
  + human jointly confirm at each close).
- The deliver-agent + human jointly agree at a milestone close to
  apply the downgrade (this is a planning-round decision, not
  automatic on the N=2 trigger).

Downgraded cases stay in the suite as regression guards. They
run automatically; auto-detection of FAIL via
`case_results[].terminal_outcome` or `composite_score` collapse
re-promotes them to `active` and triggers deliver-agent attention.

A case never automatically removes itself from the suite;
`archived` requires explicit deliver-agent + human joint decision
documented in `bad_cases/_manifest.md`.

**Bad case lifecycle:**

- **Opened** when a real session or sprint-derived finding surfaces
  a failure the deliver-agent + human agree is load-bearing.
- **Active** while the failure persists. Each milestone close
  records per-case status (PASS / FAIL / IMPROVING).
- **Closed** when the failure no longer manifests on a milestone
  rerun and the deliver-agent + human jointly confirm at milestone
  close. Closed bad cases stay in the directory as regression
  guards.

The bad-case suite directory is governance-tracked (per `doc_governance.md`
front matter equivalent); see `eval_interactive/case_specs/bad_cases/_manifest.md`
for the lifecycle ledger.
