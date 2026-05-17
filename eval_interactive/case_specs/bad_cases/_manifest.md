# Curated Bad-Case Suite

**Authored:** 2026-05-16; refined 2026-05-17 (tiering + human-judgment-gate clarification + N=2 downgrade rule)
**Source-of-truth:** this file (lifecycle ledger) + `iteration_governance.md` §5.6 / §5.6.1 / §5.6.2 / §5.6.3 (governance) + `compact/sprint-deliver-orchestrator.md` "Workflow inputs — Path 2" (operational triage + 4-route fit + edge case handling for deliver-agent)
**Owners:** deliver-agent + human (jointly maintained)

## Purpose

The bad-case suite is the **new primary acceptance gate** for sprint and milestone close per `iteration_governance.md` §5.6. It replaces the smoke composite_score / pass-rate / judge dimensions, which were demoted to observation per §5.5 due to multiple confounding sources (external LLM provider drift, judge calibration variance, mocked-vs-real-LLM gap, rubric weighting uncertainty).

**Critical: this is a human-judgment gate, NOT a programmatic gate.** Per §5.6 (2026-05-17 refinement): the deliver-agent + human MANUALLY REVIEW per-case traces; the closure_criterion field is GUIDANCE for human review, not a binary programmatic match. Early-stage, the eval rubric weights themselves are unstable; human qualitative judgment is the only reliable signal until the rubric is independently validated.

Each bad case in this directory is:

1. A real session or sprint-derived finding the deliver-agent + human jointly agree is **load-bearing** for the release-gate trajectory.
2. Encoded as a CaseSpec with the standard schema PLUS the §5.6-required `bad_case_metadata` block and `closure_criterion` field.
3. Verified by human inspection of expected behaviour from the user perspective — **NOT** by bot trace text, **NOT** by §1.7 forbidden patterns.

## Loader contract

Flat layout per Sprint 29 / Sprint 32 precedent. Loader globs `*.yaml`; this markdown file is skipped. Load + run:

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/
```

## Lifecycle ledger

| case_id | tier | source_session_id | surfaced_by | surfaced_date | current status | M1 result | M2 result | M3 result | closure_criterion summary |
|---|---|---|---|---|---|---|---|---|---|
| `alice_uc_a_uc_h_misclass` | **core** | `3772e56b-caa7-4e0a-84fc-75a26ffbe2b2` | human (Alice mock, real-LLM session) | 2026-05-16 | **active** | (in-flight) | — | — | bot routes UC-A visibility question OR asks one focused clarifying question to disambiguate visibility vs appeal, instead of stamping UC-H through the intake-locked path; bot does NOT enter a multi-turn dead loop asking "why do you think it was removed" when the user clearly does not know |

Ledger updates per milestone close: deliver-agent + human jointly record PASS / FAIL / IMPROVING per case row, citing the milestone close run path.

## Tier definitions (per `iteration_governance.md` §5.6.1)

- **`core`** — re-run at every milestone close regardless of scope. Load-bearing across all milestones (touches release-gate-relevant failure mode).
- **`scope-relevant`** — re-run only when the closing milestone explicitly names the case in its `milestone_objective.md` §5 acceptance bar. Relevant to a specific architectural surface that some milestones touch and others don't.
- **`closed-as-regression-guard`** — has met closure criterion in N ≥ 2 consecutive milestone closes (per §5.6.3 downgrade rule). Runs automatically; no human manual review unless auto-detection fires FAIL.
- **`archived`** — underlying failure surface structurally removed; no longer manifests. Removed from active runs; kept in directory as history. Requires deliver-agent + human joint decision.

## Lifecycle states (per §5.6 / §5.6.3)

- **Opened**: human + deliver-agent agree the failure is load-bearing; CaseSpec authored. Initial tier assignment (typically `core` for cross-cutting failures; `scope-relevant` for surface-specific).
- **Active**: failure persists across at least one milestone close. Each milestone records PASS / FAIL / IMPROVING per case in the ledger above.
- **`closed-as-regression-guard`**: triggered after **N ≥ 2 consecutive milestone closes with PASS**. Downgrade is NOT automatic on the N=2 trigger — deliver-agent + human must jointly confirm at a milestone close to apply the downgrade. After downgrade, case runs in suite automatically; if `case_results[].terminal_outcome` returns to FAIL OR `composite_score` collapses, the case auto-promotes back to active.
- **Archived**: structurally impossible. Requires explicit joint decision; documented in this ledger.

## Adding a new bad case (Path 2 — operational steps in compact)

When a new bad case surfaces (human use / colleague use / sprint-surfaced finding):

1. Human + deliver-agent confirm it is load-bearing per §5.6 criteria:
   - Influences release-gate trajectory.
   - Failure mode crosses ≥ 1 layer (not a single-component cosmetic).
   - Reproducible OR represents a typical scenario class.
   - Not a duplicate of an existing closed/archived case.
2. Per Path 2: trace + observation are handed to a research-agent (not directly to deliver-agent) for root-cause analysis + solution proposal. See `compact/sprint-deliver-orchestrator.md` "Workflow inputs — Path 2" for the full operational process (triage gate → research-agent proposal → encode → 4-route fit decision → downstream milestone loop).
3. After research-agent proposal lands and human selects the design, deliver-agent authors a new `<case_id>.yaml` here using the schema in any existing bad case + the §5.6 extension fields.
4. Deliver-agent assigns initial tier (`core` for cross-cutting; `scope-relevant` for surface-specific). Appends a row to the lifecycle ledger above.
5. Deliver-agent decides whether the case fits the current milestone scope (add to `milestone_objective.md` §5 acceptance bar) OR is queued for a future milestone (open as an R-item in `docs/action_bank.md` referencing the bad case).

A bad case can also be opened WITHOUT a Path 2 proposal — when the human + deliver-agent simply want to encode the observation as a regression guard without immediate fix scoping. In that case, the case enters with tier `scope-relevant` and is consumed by a future milestone whose scope reaches the relevant surface.

## Removing a bad case

Bad cases are NOT removed at closure; they stay as regression guards. Removal is only acceptable if the underlying failure mode becomes structurally impossible (e.g., the surface that produced it is deleted), and requires deliver-agent + human joint decision documented in this lifecycle ledger.

## Why this is not in `case_specs/case_families/`

The case-family directory follows the Sprint 20 G2 target / neighbor / negative / shadow split shape. The bad-case suite is a different artefact: one case per real surfaced failure, no neighbor / negative / shadow split required (the case itself IS the load-bearing evidence). Mixing the two schemas would dilute both contracts. Per Sprint 29 `_manifest.md` precedent (local manifest, intentionally not in root manifest), the bad-case suite is local-only.
