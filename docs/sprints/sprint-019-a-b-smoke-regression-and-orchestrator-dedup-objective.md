---
title: Sprint 19 — Smoke Regression Investigation + Orchestrator Tool-Call De-Dup (parallel A + B)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-13
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 19 is an investigation-class sprint with bundled-fixes-allowed
  policy on each per-case clean layer finding. Two disjoint tracks run
  in parallel under one sprint scope. Section 7 stanza is in multi-layer
  prospective form (Track A spans judge_calibration / semantic_planner /
  prompt_projection / human_review_required per per-case finding;
  Track B is infra). See §"Bundle-or-defer policy" for the rule the dev
  agent applies per finding. Sprint 19 must complete before G2 case-
  family construction; G2 has no clean baseline until Track A closes.
---

# Sprint Objective

Date: 2026-05-13

## Sprint name

Sprint 19 — Smoke Regression Investigation + Orchestrator Tool-Call
De-Dup (parallel A + B)

## Goal

Resolve the two R-items in the Sprint 18 G1 backlog that block the G2
case-family construction:

- **Track A — `R-smoke-regression-investigation` (P1).** Diagnose the
  64 % → 21 % smoke-set drop between `eval_interactive/results/20260505-235231/`
  and `eval_interactive/results/20260510-134558/` over the 6 regressed
  cases (cs_002, cs_011, cs_014, cs_038, cs_040, cs_066). Sprints 14 /
  14.1 / 15 / 16 all declared "no runtime semantic change"; the dev
  agent walks `docs/current/iteration_governance.md` §3.2 per case and
  identifies the matching layer. Per-case bundle-or-defer per §"Bundle-
  or-defer policy" below.
- **Track B — `R-runtime-orchestrator-tool-call-deduplication`.** From
  manual-probe 2026-05-13: 1 LLM request → 3 identical
  `search_knowledge` executions, same params and same results.
  Investigate the 3 hypotheses (phase-transition re-trigger, Turn 1
  failed-call replay, LLM new request), document the orchestrator's
  intended de-dup / idempotency contract, and — if the per-case layer
  resolves to `infra` — bundle the fix with a regression test. If the
  layer resolves to `semantic_planner` (LLM emitted three identical
  calls), defer the fix and produce a remediation proposal only.

The two tracks are **disjoint** (Track A diagnoses the smoke baseline;
Track B diagnoses orchestrator transport). The dev agent may interleave
them in one PR or split into two; either is acceptable.

## Background

Sprint 18 G1 closed with `decision: <no Codex review run; deliver-direct
authoring>` and surfaced 18 R-items + 2 open observations
(`docs/sprints/sprint-018-handoff.md`, commit `18a96ed`). Two of those
items must close before G2:

1. The smoke baseline is unstable — G2 case-family construction over
   target / neighbor / negative / shadow requires a clean reference
   run, which the 2026-05-10 21 % rerun does not provide.
2. The manual-probe `search_knowledge` triple-execution is the first
   evidence of an orchestrator transport anomaly that may corrupt
   trace evidence the G2 shadow split would later rely on.

Sprint 19 closes both. G2 (Interactive Eval Case Family + Shadow Split)
follows Sprint 19; it does not run inside Sprint 19.

## Tracks (in scope)

### Track A — Smoke Regression Investigation

**Input artefacts:**

- `eval_interactive/results/20260505-235231/results.json` (label
  `sprint8-r2`; 9 / 14 pass).
- `eval_interactive/results/20260510-134558/results.json` (label
  `smoke_rerun_20260510-214558`; 3 / 14 pass).
- 6 regressed cases: cs_002, cs_011, cs_014, cs_038, cs_040, cs_066.
  Symptoms span empty `escalation_reason`, `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP`,
  `turn_budget_exhausted`, UC mis-route,
  `CONTRACT_VIOLATION:active_use_case`.
- 8 non-regressed smoke cases (the other 8 of the 14): treated as
  neighbor cases — must not have regressed in the opposite direction
  during any fix bundled in this sprint.

**Required per-case walk (per `iteration_governance.md` §3.2):**

For each of the 6 regressed cases, the dev agent records:

- The two trace excerpts (2026-05-05 PASS, 2026-05-10 FAIL).
- The first-match-wins §3.2 question that triggers.
- The classified layer from §3.1.
- Whether a reversible fix is bundled this sprint OR deferred to a
  remediation proposal — per §"Bundle-or-defer policy" below.

**Special-case stop-and-escalate (cs_192 shape):** if any per-case walk
surfaces a `CONTRACT_VIOLATION:active_use_case` that would re-open
Sprint 8 §K0 territory, the dev agent **stops bundling** and asks for
human Tier-0 review per §3.2 Q2's "no current Tier-0 → flag
`human_review_required`" rule. Do not invent a new Tier-0 invariant.
cs_192 itself is not in the 6 regressed cases, but it is the
canonical shape this rule guards against.

### Track B — Orchestrator Tool-Call De-Dup

**Input artefact:** `manual-probe-2026-05-13` brief (session
`4a2f3680-02a8-4d13-8f0b-f99d7c249b57`) at
`docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`.

**Required investigation:**

- Read the manual-probe trace; locate the 3 `search_knowledge`
  executions and confirm parameter / result identity.
- Walk the 3 hypotheses (phase-transition re-trigger, Turn 1 failed-call
  replay, LLM new request) against the trace and the
  AgentRunLoop / ToolDispatcher code paths. Cite specific code paths.
- Document the orchestrator's intended de-dup / idempotency contract
  in the handoff (what the contract is today; what it should be).
- Walk §3.2 to classify: typically `infra` per Q1, but if the trace
  shows the LLM itself emitted 3 separate `tool_use` blocks it is
  `semantic_planner`.

## Bundle-or-defer policy

When the per-case (Track A) or whole-track (Track B) layer walk yields
a clean layer below, the dev agent **MAY** bundle a reversible fix in
the same PR. Otherwise, the dev agent produces a remediation proposal
only.

**Bundle allowed (reversible fix):**

- `infra` — orchestration, transport, persistence, idempotency,
  timeouts, OOM, endpoint / credential wiring. Reversible means a
  small targeted change with a regression test attached. The Track B
  tool-call dedup, if `infra`, is the canonical bundle.
- `judge_calibration` — narrow rubric-stability fix that does **not**
  widen the rubric to accept a genuine bot mistake (forbidden by
  Constitution §1.7). Allowed: tightening a judge configuration knob
  that flips across reruns; **not allowed**: relaxing an L1 / L2 / L3
  bar.
- `prompt_projection` — adding a **soft signal** (an additional
  projected slot, a candidate list, a diagnostic flag) that the LLM
  may consume. **Not** a prompt if-else, **not** a keyword / regex
  branch in projection assembly.

**Defer (remediation proposal only):**

- `semantic_planner` — the LLM's own semantic choices. Fixes here
  require shadow case coverage that Sprint 19 does not have. Defer
  to a G3+ runtime sprint.
- `eval_spec` — CaseSpec / rubric edits go through Wave A5 / A6 L3
  review, not this sprint.
- `product_policy` — requires product sign-off.
- `human_review_required` — surface to human; do not invent a Tier-0.
- `skill_state` — multi-turn state changes touch the semantic surface
  the G2 case family is supposed to exercise; defer to G2 / G3+.
- `java_guard` — only justified by a current Tier-0 invariant. If the
  finding looks like Java-guard territory and no current Tier-0
  covers it, flag `human_review_required` (do not invent a new
  Tier-0). This is the cs_192 stop-and-escalate clause restated.

**Bundle hard rules:**

- No keyword / regex / if-else / enum / per-UC matrix added for any
  semantic decision in any bundled fix.
- No CaseSpec or override edit in any bundled fix.
- No prompt edit beyond pure projection (a new projected slot wired
  through the existing projection assembly is allowed; a new prompt
  paragraph or if-else is not).
- Every bundled fix carries a regression test.

The dev agent records its bundle / defer decision and the §3.2
walk per case in the Sprint 19 handoff.

## Layer-classification + anti-hardcode stanza

**Target failure layer:** **multi-layer prospective.** Track A's per-
case walk may resolve, per case, to any of: `judge_calibration` |
`semantic_planner` | `prompt_projection` | `human_review_required`.
(Pre-investigation `infra` for Track A is implausible — the smoke
harness ran end-to-end on both dates; `skill_state` and `eval_spec`
are possible but the per-case walk decides.) Track B is **`infra`**
prospectively per `iteration_governance.md` §3.2 Q1; if the trace
shows the LLM itself emitted 3 separate `tool_use` blocks, the layer
re-resolves to `semantic_planner` and Track B's fix is deferred.

The dev agent walks `iteration_governance.md` §3.2 per regressed case
(Track A) and once for Track B, and records the matching layer in the
Sprint 19 handoff layer-classification self-walk section.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.
`iteration_governance.md` §3.2 Q2 is the only branch that re-classifies
a finding as `java_guard`; per the §"Bundle-or-defer policy" hard rule,
any `java_guard`-looking finding without a current Tier-0 invariant
flags `human_review_required` rather than introducing one. The cs_192
`CONTRACT_VIOLATION:active_use_case` shape is the canonical case this
clause guards against (Sprint 8 §K0 re-open territory).

**Semantic hardcode:** No semantic hardcode introduced. Per
§"Bundle-or-defer policy", bundles forbid keyword / regex / if-else /
enum / per-UC-matrix additions; any finding whose only fix would be
such an addition is deferred to a G3+ runtime sprint with a written
remediation proposal. `prompt_projection` bundles add **soft signals
only** (additional projected slots / candidate lists / diagnostic
flags) — never prompt if-else or projection-time regex branches.
`judge_calibration` bundles do **not** widen rubrics to accept a
genuine bot mistake (Constitution §1.7).

**Generalization coverage:** target = the 6 regressed cases (cs_002,
cs_011, cs_014, cs_038, cs_040, cs_066) for Track A plus the
manual-probe-2026-05-13 trace for Track B; neighbor = the 8 non-
regressed smoke cases (must not regress in the opposite direction
during any bundled fix); negative = at least 2 cases that should **not**
re-pass post-fix (the dev agent identifies these from the smoke set
when proposing each bundle, so the bundle is provably narrow); shadow
= deferred to G2 (case-family + shadow split is the next sprint after
Sprint 19; Sprint 19 does not construct shadow). The shadow gap is
intentional and explicit per `iteration_governance.md` §5.1's allowance
for sprints that pre-date the shadow split.

## Do not implement

- Any change under `eval/`, `eval_interactive/case_specs/`,
  `eval_interactive/case_spec_overrides.yaml`, or
  `eval_interactive/personas*.yaml`. CaseSpec / override edits go
  through Wave A5 / A6 L3 review, not this sprint.
- Any FAQ corpus edit (`data/faq/*`, `FAQ-knowledge_include_help_url.csv`).
  `R-corpus-coverage-audit-per-uc` is deferred.
- Any prompt edit beyond pure projection (no new prompt paragraph,
  no prompt if-else, no system_prompt copy edit). A new projected
  slot wired through existing projection assembly is allowed for a
  `prompt_projection` bundle.
- Any judge rubric edit that widens an L1 / L2 / L3 bar to accept
  the bot's actual output (Constitution §1.7).
- Any keyword / regex / if-else / enum / per-UC matrix added to
  runtime or prompt for a semantic decision. The bundle policy
  forbids these; a finding whose only fix is such an addition must
  be deferred.
- Any new Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md`.
  cs_192-shaped findings flag `human_review_required`; the human
  decides whether to open a new Tier-0.
- Any work on the other 16 R-items in `docs/action_bank.md` §5.2.
  Wave A5 / A6 L3 review batch, corpus audit, faqMissCount threshold,
  duplicated greeting, etc. are explicitly out of scope.
- Any G2 case-family construction (target / neighbor / negative /
  shadow split). G2 follows Sprint 19.
- Any edit to `docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`,
  `docs/foundational/*`, or `docs/sprints/*` (the Sprint 19 archive
  copies in `docs/sprints/` are written by the deliver agent at
  sprint close, not by the dev agent).
- Any architecture-health metric collection. Per §6, the four
  metrics remain `collection_status: not_started`; Sprint 19 does
  not open collection.
- Any change to `docs/codex-findings.md` by the dev agent. The Codex
  review writes this file at sprint close.

## Success metrics (per `iteration_governance.md` §5.1)

- **Target cases addressed.** For each of the 6 regressed cases
  (Track A) and the manual-probe trace (Track B): a §3.2 walk in
  the Sprint 19 handoff and a bundle-or-defer decision per the
  §"Bundle-or-defer policy" rule. Cases whose layer is `bundle-
  allowed` (above) and whose fix is bundled must show a passing
  regression test in the PR.
- **Neighbor cases no regression.** Smoke rerun after bundled fixes
  shows no regression on the 8 non-regressed smoke cases. If no fix
  is bundled (proposal-only sprint), this bar is trivially satisfied.
- **Negative-control no false positive.** For each bundled fix, the
  dev agent names ≥2 negative-control cases the fix must **not**
  start passing. The smoke rerun confirms.
- **Shadow no regression.** Sprint 19 pre-dates the shadow split.
  This bar is deferred per `iteration_governance.md` §5.1's shadow
  allowance and is explicitly named in the handoff.
- **Safety floor unchanged.** Tier-0 safety invariants (PII, safety,
  identity verification, imminent harm) remain green. The dev agent
  confirms in the handoff.
- **Grounding floor unchanged.** FAQ grounding diagnostics per
  `docs/current/faq_grounding_contract.md` (six output classes,
  citation diagnostics) remain at or above their prior level.
- **Wrong-containment rate unchanged or down.** Sessions contained
  in the wrong UC / wrong phase / with no grounded answer must not
  increase.
- **Over-escalation rate unchanged or down.** `request_handover`
  rate on cases that should resolve must not increase.
- **Architecture-health metrics not regressed.** §6 metrics remain
  `collection_status: not_started`; this bar is trivially satisfied
  for Sprint 19.

## Review rule

Codex runs the Anti-Hardcode Review (`iteration_governance.md` §4.1)
on the Sprint 19 PR diff and writes its decision to
`docs/codex-findings.md` using the §4.2 sprint-close 4-line header
(`## Sprint Review Decision` / `decision:` / `blocking_count:` /
`summary:`).

Codex is told explicitly (in the review prompt) that a bundled `infra`
fix (Track B) or a bundled narrow `judge_calibration` / soft-signal
`prompt_projection` fix (Track A) is **not scope drift**. The
investigation framing of Sprint 19 contemplates exactly these bundles
per the §"Bundle-or-defer policy" above. Out-of-scope concerns Codex
surfaces (e.g. opinions on the other 16 R-items, opinions on G2 design)
are recorded in `docs/action_bank.md` as deferred items rather than as
blocking findings.

`fix_required` is reserved for concrete §4.1-failing findings on the
Sprint 19 diff: a bundle that introduces a forbidden hardcode, a
bundled fix without a regression test, a `human_review_required`
finding that was silently treated as a bundle, a `java_guard` finding
that the dev agent introduced without a Tier-0 invariant, or a per-case
§3.2 walk missing for any of the 6 regressed cases.

## Deliverables (what the dev-agent PR must contain)

- A single PR on a fresh branch (recommended: `sprint-019-a-b-parallel`
  or similar).
- Per-case Track A handoff section: §3.2 walk for each of the 6
  regressed cases + bundle / defer decision + (if bundled) the test
  evidence.
- Track B handoff section: 3-hypothesis investigation + orchestrator
  de-dup / idempotency contract write-up + bundle / defer decision +
  (if bundled) regression test.
- A 12-section Sprint 19 handoff at `docs/sprints/sprint-019-handoff.md`
  following the Sprint 17 / Sprint 18 precedent (Context Pack, exact
  actions, files changed, layer-classification self-walk, anti-hardcode
  self-walk, generalization-coverage table, sprint-objective-met
  check, open questions, next recommended action, etc.).
- Updated `docs/10-handoff.md` leading with Sprint 19.
- No edit to `docs/sprint_objective.md`, `docs/sprints/*`, or any
  archived file. The deliver agent archives at sprint close.

Codex writes its review to `docs/codex-findings.md` after the PR is
open, per the review prompt at `compact/sprint-019-review-prompt.md`.
