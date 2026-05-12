# Sprint Objective

Date: 2026-05-13

## Sprint name

Sprint 18 — Human-led Failure Portfolio (G1)

## Goal

Convert 10 representative real failures into structured Failure Briefs
under `docs/diagnostics/failure-briefs/`, populate the deferred-backlog
ledger with the R-items those briefs surface, and add a Method note to
the Sprint 18 handoff documenting the two ground-truth derivation
techniques the briefs depend on (CaseSpec L3 override check; manual-
probe ground-truth from authoritative authoring sources).

This is a **docs-only governance sprint** (G1 in the research agents'
G0 → G1 → G2 → G3+ sequencing — see Sprint 17 G0's background section).
It must not change runtime, prompt, FAQ corpus, CaseSpec, judge, eval
harness, tests, or scripts. It must not propose fixes. Briefs name the
likely-responsible layer per `docs/current/iteration_governance.md` §3
and the tempting-but-wrong fix per the §1.5 Iteration rule; remediation
itself is the work of G3+ runtime sprints (gated by G2 case families
first).

The scope is intentionally narrow: G1 is the human-led failure
catalogue. Brief authoring is the deliverable. Per-case fixes, case
families, shadow splits, and runtime change are explicitly out of
scope and live in G2 / G3+.

## Background

The research agents converged on the following sequencing:

1. **G0 — Iteration Governance Lite** (Sprint 17, closed Codex pass):
   docs-only governance scaffolding. Landed
   `docs/current/iteration_governance.md` Sections 1–7, seeded
   `AGENTS.md` constitution chain, opened
   `docs/diagnostics/failure-briefs/` as the directory G1 will
   populate.
2. **G1 — Human-led Failure Portfolio** (this sprint): pick
   representative failures from the human's experience plus the most
   recent smoke runs and convert each into a Failure Brief per
   `docs/current/iteration_governance.md` §2.
3. **G2 — Interactive Eval Case Family + Shadow Split** (deferred to
   next governance sprint): turn each G1 brief into a target /
   neighbor / negative / shadow case family with the shadow split
   readable only to human / review agent per
   `docs/current/iteration_governance.md` §5.1.
4. Only after G0 → G1 → G2 do we re-open Semantic Planner shadow mode
   and runtime work (G3+).

Repo state at the start of G1:

- Sprint 17 (G0) closed with Codex `decision: pass, blocking_count: 0`
  (commit `ac778bc`). The 7-section governance bundle is live; the
  Section 2 Failure Brief Template is the template G1 follows.
- `docs/diagnostics/failure-briefs/` is empty / not-yet-created at G0
  close; G1 creates it and populates 10 briefs.
- The two most recent smoke runs (`20260505-235231` and
  `20260510-134558`) over the same 14-case interactive smoke set are
  the primary trace input for the smoke briefs. Run 1 was 9 / 14
  pass, run 2 was 3 / 14 pass; the regression itself is flagged as
  `R-smoke-regression-investigation` and deferred (Cluster C, see
  §Framing C below).
- The Wave A6 / A6.6 L3 override pipeline (per
  `docs/proposals/interactive_case_spec_generation_plan.md`) is the
  authority that distinguishes human-reviewed CaseSpec ground truth
  from raw rule-extracted ground truth. Several G1 briefs cite a
  Wave A5 / A6 override as their authority; the briefs that do not
  flag the underlying CaseSpec as a candidate for L3 triage.

## Implement exactly these 5 actions

### G1.1 — File 10 Failure Briefs under `docs/diagnostics/failure-briefs/`

Each brief covers one representative failure shape and follows the
6-field structure defined in
`docs/current/iteration_governance.md` §2 (What happened? / What
should a good CS agent have done? / Why does this matter? / Is this a
one-off or a pattern? / Which layer is likely responsible? / What
should NOT be done?), plus header metadata (source case,
source_session_id, CaseSpec path, approved L3 override status, runs,
filed date), plus an optional Ground-truth chain preamble when a Wave
A5 / A6 override exists or when ground truth must be derived from
non-CaseSpec sources, plus an optional Related observation tail for
tangential phase 2 / eval_spec / corpus / R-item findings.

The 10 briefs split as **9 smoke briefs + 1 manual-probe brief**:

| # | brief filename | source | cluster |
|---|----------------|--------|---------|
| 1 | `cs015-uc-fp-mis-route-and-premature-escalate.md` | smoke | A (persistent fail) |
| 2 | `cs001-uc-c-template-escalate-on-faq-miss.md` | smoke | B-1 (disengaged-template) |
| 3 | `cs011-uc-d-detailed-description-ignored-on-faq-miss.md` | smoke | B-1 (user-detail-ignored extreme) |
| 4 | `cs038-uc-j-intake-redundancy-and-jargon-framing.md` | smoke | B-2 (engaged-but-mechanical) |
| 5 | `cs040-uc-k-disengaged-jargon-intake-false-complete.md` | smoke | B-3 (disengaged + jargon hybrid) |
| 6 | `cs095-uc-classification-and-account-aware-path-skipped.md` | smoke | A (persistent fail) |
| 7 | `cs176-uc-e-wrong-escalation-reason-family.md` | smoke | A (persistent fail) |
| 8 | `cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md` | smoke | A (persistent fail; mechanical surface) |
| 9 | `cs259-uc-f-sprint7-i0-violation-on-payment-question.md` | smoke | A (persistent fail) |
| 10 | `manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md` | manual probe | — (multi-layer, single-trace) |

Filename convention α: `<case_id>-<uc>-<slug>.md` for smoke briefs;
`manual-probe-<date>-<slug>.md` for the manual probe brief.

### G1.2 — Append 18 R-items + 2 open observations to `docs/action_bank.md`

Append a new sub-section `§5.2 G1 surfaced backlog` to
`docs/action_bank.md` capturing the 18 R-items the briefs surface,
plus the 2 open observations that are *not* opened on n=1 evidence
(n=1 evidence is insufficient per the cs_259 / manual-probe rule;
see the Sprint 18 handoff for the full reasoning).

R-item composition:

- 1 Tier-0 candidate (`R-escalation-reason-runtime-evidence-contract-review` — 3 instances; solidly systematic)
- 5 systematic (≥2 instances each: generator-vs-policy mismatch, L3 judge form-context-trust, corpus coverage audit, faqMissCount threshold/timing, duplicated greeting)
- 9 per-case L3 / governance items (1 per affected brief, plus 1 conditional)
- 1 G2 input item (multi-turn followup case family design)
- 1 new infra item (manual-probe orchestrator tool-call dedup)
- 1 external / regression discovery item (`R-smoke-regression-investigation`; pre-existing, formally restated here)

The 2 open observations track:

- bot ignores explicit phase-plan directives (cs_259 + manual-probe —
  n=2 opportunistic; controlled multi-shape testing needed before
  opening as R-item)
- ad_id form-vs-listing data consistency (manual-probe only; n=1;
  needs production data)

### G1.3 — Mark `docs/action_bank.md` §5.1 G1 row as `done`

Update the G1 row from `deferred — next governance sprint` to
`done — 10 briefs filed (9 smoke + 1 manual-probe); see
docs/diagnostics/failure-briefs/`. G2 remains deferred. No other
§5.1 entry changes.

### G1.4 — Document the Method note in the Sprint 18 handoff

The Sprint 18 handoff includes a Method note section documenting the
two ground-truth derivation techniques the briefs used:

- **CaseSpec L3 override check.** Before filing a Failure Brief on a
  generated CaseSpec, check `eval_interactive/case_spec_overrides.yaml`
  for an approved L3 override keyed by `source_session_id`. If
  present, anchor the brief's expected behaviour on the override's
  `classification.*` / `expected.*` blocks. If absent, the CaseSpec
  is L1 rule-extracted (plus optional L2 persona review) and the
  brief should flag whether the CaseSpec itself needs L3 triage as a
  candidate `eval_spec` failure per §3.2 Q6.
- **Manual-probe ground-truth chain.** For traces with no CaseSpec
  (manual probe or production capture), derive expected behaviour
  from authoritative authoring sources: phase 2 UC policy +
  `phase_plan.system_instruction` text the bot itself receives at
  the failing turn. Both are checked into the repo / observable in
  the trace; both are written by humans for the agent to consume.
  Document the derivation chain in the brief's Ground-truth chain
  preamble.

These techniques are documented in the handoff (G1.4) rather than in
`docs/current/iteration_governance.md` §2 to avoid expanding the
governance doc mid-sprint; if the techniques prove durable, fold-back
to §2 happens on the normal `iteration_governance.md` cadence (every
3–5 sprints).

### G1.5 — Archive Sprint 18 G1 objective + write Sprint 18 handoff

This `docs/sprint_objective.md` is copied verbatim to
`docs/sprints/sprint-018-g1-failure-portfolio-objective.md` as the
sprint archive. The full Sprint 18 handoff lands at
`docs/sprints/sprint-018-handoff.md` and follows the Sprint 17 G0
handoff section structure (Context Pack, exact actions implemented,
files changed, layer-classification self-walk, anti-hardcode
self-walk, Method note, sprint-objective-met check, open questions,
next recommended action).

No Codex review file is created for G1. Per the human's Q3 packaging
decision (option 2, recorded in `compact/sprint-deliver-orchestrator.md`
§4.3), G1 brief authoring is done by the deliver agent + human jointly
in chat; no dev agent and no Codex review agent are run on G1.
`docs/codex-findings.md` retains its Sprint 17 G0 content until the
next sprint that runs a Codex review.

## Framing C (scope decision; recorded for the archive)

Three framings were considered before brief authoring:

- **Framing A** — file 10–20 briefs covering both smoke runs plus
  human-experience failures. Risk: too broad, no clean baseline
  because the 64% → 21% run-to-run regression confounds clusters.
- **Framing B** — open a G0.5 regression-investigation sprint before
  G1 so G1 starts from a stable baseline. Risk: G0 → G0.5 → G1 → G2
  serializes too aggressively; the regression itself is a finding
  G1 can surface and defer.
- **Framing C (chosen)** — narrow G1 to 9 smoke briefs (5 persistent
  failures from Cluster A + 4 representative mechanical-surface
  briefs from Cluster B) + 1 manual-probe brief. The 6 regressed
  cases (Cluster C: cs_002, cs_011, cs_014, cs_038, cs_040, cs_066)
  are not separately briefed; the regression is surfaced as
  `R-smoke-regression-investigation` and deferred. cs_011 / cs_038 /
  cs_040 already have Cluster B briefs that cover the mechanical
  surface; cs_002 / cs_014 / cs_066 are deferred to G2 as neighbor
  cases on the existing Cluster B representatives.

Framing C was approved by the human before brief authoring started.

## Layer-classification + anti-hardcode stanza — EXEMPT

Per `docs/current/iteration_governance.md` §7, the stanza is required
for **semantic-touching sprints**: sprints that change prompt, a
runtime semantic decision (UC routing, drift detection, escalation
posture, follow-up policy), the eval spec, or judge calibration.

Sprint 18 G1 is **docs-only governance** (Failure Brief authoring +
action-bank ledger update + Method note). It changes no prompt, no
runtime semantic code, no CaseSpec, no eval spec, no judge config.
It surfaces R-items naming candidate layers for later sprints; it
does not itself touch those layers.

Sprint 18 therefore declares the §7 stanza **exempt** under the same
exemption Sprint 15 (config governance) and Sprint 16 (docs +
characterization tests) used. The exemption is named explicitly here
so a future deliver-agent review does not over-apply the stanza to
G1-style human-led brief authoring sprints.

## Do not implement

- Any change under `server/`, `ui/`, `eval/`, `eval_interactive/`,
  `data/`, `scripts/`, or root config files.
- Any prompt edit (`system_prompt.txt`, routing prompts, judge prompts).
- Any FAQ corpus / CaseSpec / judge / eval-output schema change.
- Any CaseSpec override edit (`eval_interactive/case_spec_overrides.yaml`).
  The L3 override pipeline is Wave A5 / A6 / A6.6's responsibility,
  not G1's. Briefs may flag CaseSpec candidates for L3 re-review
  (e.g. `R-cs001-escalation-trigger-l3-review`,
  `R-cs095-uc-classification-l3-rereview`,
  `R-cs176-escalation-reason-l3-review`); the L3 work itself happens
  in a later sprint.
- Any new test (Java, Python, or eval harness).
- Any per-case fix or runtime patch. Briefs name the likely-responsible
  layer per §3 and the tempting-but-wrong fix per §1.5; remediation is
  the work of G3+ runtime sprints, gated by G2 case families first.
- Any G2 case family construction (target / neighbor / negative /
  shadow). G2 depends on G1 briefs as input and runs as a separate
  governance sprint.
- Any architecture-health metric collection. Per §6, the four metrics
  are defined-only with `collection_status: not_started`; G1 does not
  open collection.
- Any edit to `docs/sprints/*` archived sprints.
- Any edit to `docs/foundational/*` (phase docs, normative freezes).
- Any edit to `docs/current/iteration_governance.md`. The Method note
  lives in the Sprint 18 handoff, not in §2; if it proves durable,
  fold-back happens on the §2 cadence (every 3–5 sprints).
- Any edit to `docs/current/doc_governance.md` or
  `docs/current/agent_context_guide.md`.
- Any update to `docs/codex-findings.md`. No Codex review is run for
  G1; the file retains its Sprint 17 G0 content.
- Any update to `docs/current_eval_baseline.md`. G1 does not run eval;
  the canonical baseline is unchanged.
- Any new tier or new front-matter status. Reuse existing
  `doc_governance.md` enums.

## Success metrics

- `docs/diagnostics/failure-briefs/` contains exactly 10 brief files,
  each with the 6 required fields from
  `docs/current/iteration_governance.md` §2, each naming exactly one
  primary layer from the §3.1 layer set (multiple-candidate layers
  allowed, per §2 brief-field 5).
- Each brief whose CaseSpec is anchored by a Wave A5 / A6 approved L3
  override cites the override file + line range and rules out
  `eval_spec` as a candidate layer for the outcome failure (the
  override is the human-reviewed ground truth).
- Each brief whose CaseSpec has no approved L3 override flags the
  CaseSpec as a candidate for L3 triage in its Ground-truth chain
  preamble OR in its What should NOT be done? field (no silent
  CaseSpec-is-authority assumption).
- The manual-probe brief derives ground truth from phase 2 UC policy
  + the bot's own `phase_plan.system_instruction` (both authoritative
  authoring sources). The brief documents the derivation chain in its
  preamble.
- `docs/action_bank.md` §5.2 (new sub-section) lists all 18 R-items
  with stable kebab-case ids and one-line descriptions, plus the 2
  open observations.
- `docs/action_bank.md` §5.1 G1 row is updated to `done`; G2 remains
  `deferred`.
- The Sprint 18 handoff includes the Method note (G1.4) and the
  layer-classification self-walk required by Sprint 17 G0 §3
  precedent.
- `mvn -pl server test` and `pytest eval_interactive/tests/` are not
  required to be re-run (no code touched). The handoff explicitly
  states "no code touched, no tests run".
- No file under `server/`, `ui/`, `eval/`, `eval_interactive/`,
  `data/`, `scripts/`, `docs/foundational/`, `docs/current/`, or
  `docs/sprints/` (other than the new Sprint 18 archive copies) is
  modified.

## Review rule

No Codex review is run for G1 per the human's Q3 packaging decision
(option 2). The deliver agent + human jointly authored the briefs in
chat; the packaging commit captures the human-approved end state.

If a future review pass is opened on G1 (deferred decision), the
reviewer would check:

1. Each brief has the 6 fields with no empty field.
2. Each brief's named primary layer is reachable by walking
   `iteration_governance.md` §3.2's 7 first-match-wins questions
   against the What happened? + Why does this matter? content.
3. Each brief's What should NOT be done? names a concrete
   keyword / regex / if-else / enum / per-UC matrix and cites the
   Constitution clause it violates.
4. R-items in `docs/action_bank.md` §5.2 are traceable to ≥1 brief
   each (n=1 → per-case R-item is allowed; n≥2 → systematic R-item
   is required, applying the conditional-broadening rule recorded in
   the Sprint 18 handoff §6).
5. The Method note in the Sprint 18 handoff is consistent with
   `iteration_governance.md` §2 and §3.2 Q6 (eval_spec layer
   selection).
6. The diff stays inside docs (no `server/` / `eval/` / etc.).
7. The Sprint 17 G0 archive is not edited.

If the review finds a missing field in any brief, the expected
`decision` is `fix_required` with a named brief + the missing field.
If the review finds an R-item with no source brief, the expected
`decision` is `fix_required` with the R-item id named. If the review
finds the diff inside scope and the briefs internally consistent,
the expected `decision` is `pass`.
