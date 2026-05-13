---
title: Sprint 20 — G2 Interactive Case Family + Shadow Split (Track A) + Already-Called Soft Signal (Track B)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-13
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 20 runs two disjoint tracks under one sprint scope, mirroring
  Sprint 19's A+B precedent. Track A is the G2 meta-sprint — case-family
  authoring (target / neighbor / negative / shadow split) for the 10
  G1 Failure Briefs; Track A explicitly does NOT remediate any failure
  it catalogues. Track B is the read-side soft-signal slot for
  `R-prompt-projection-already-called-soft-signal`. Both tracks are
  semantic-touching (Track A sits on the eval-spec surface; Track B is
  `prompt_projection`); the §7 stanza is REQUIRED and is filled in
  multi-layer prospective form per the
  `feedback_multi_layer_prospective_stanza` precedent. See
  §"Bundle-or-defer policy" for Track B's rule; Track A is
  content-authoring and is not subject to bundling.
---

# Sprint Objective

Date: 2026-05-13

## Sprint name

Sprint 20 — G2 Interactive Case Family + Shadow Split (Track A) +
Already-Called Soft Signal (Track B)

## Goal

Two disjoint tracks under one sprint scope, ordered by priority:

- **Track A — G2 Interactive Case Family + Shadow Split.** Convert
  the 10 G1 Failure Briefs at `docs/diagnostics/failure-briefs/` into
  the four-class case-family structure required by
  `docs/current/iteration_governance.md` §5.1 (target / neighbor /
  negative / shadow). Authoring only; **no remediation of any
  brief's underlying failure.** Build the shadow-split mechanism that
  hides shadow-class cases from the dev agent while keeping them
  readable to the human and the review agent.
- **Track B — `R-prompt-projection-already-called-soft-signal`.**
  Surface an `already_called: [{tool, arguments_hash, at_step}]`
  diagnostic slot in the per-step projection so the LLM can see
  "you already called this tool with these args" before re-emitting.
  Soft signal only. The LLM owns whether to re-emit. Includes a
  regression test that demonstrates the slot is populated and that
  no Java check enforces consumption.

The two tracks are **disjoint** (Track A is data authoring on the
eval surface; Track B is a `server/**` projection-assembly addition).
The dev agent may interleave them in one PR or split into two; either
is acceptable. Track A is the primary track and must close even if
Track B is deferred mid-sprint.

## Background

Sprint 19 (closed 2026-05-13, Codex `pass / blocking_count: 0`)
diagnosed the smoke-baseline regression as multi-shape (slow-LLM
placeholder loop + UC-K state-loss + planner failure to escalate-on-
explicit-request) and reclassified the orchestrator de-dup R-item
from `infra` to `semantic_planner`, splitting remediation into a
read-side soft signal (this sprint's Track B) and a write-side
HandoverOrchestrator design. With the smoke-baseline annotations
in `docs/sprints/sprint-019-handoff.md` §3 + §4, G2 case-family
construction can now proceed: the regression-shape annotations tell
G2 which 2026-05-10 fail tags to treat as baseline noise (the
slow-LLM and case_id-binding shapes) vs which to model as semantic
target/neighbor/negative case families (the Cluster A persistent
failures from G1).

Sprint 20 closes one G1-surfaced read-side proposal (Track B) and
delivers the meta-sprint for generalization (Track A). G3+ runtime
remediation (`R-slow-llm-placeholder-coalesce`,
`R-uc-k-intake-complete-case-id-binding`,
`R-prompt-phase-plan-directive-followship`, the per-case L3 review
batch, etc.) waits on Track A's case families.

## Tracks (in scope)

### Track A — G2 Interactive Case Family + Shadow Split

**Authoring scope (per the 10 G1 briefs).** For each of the 10 G1
briefs at `docs/diagnostics/failure-briefs/`, produce a case family
under the appropriate `eval_interactive/case_specs/` sub-directory
(the dev agent's Context Pack must propose the exact directory; the
human reviews before authoring). Each family contains:

- **Target case.** The case the brief catalogues. For 9 of the 10
  briefs (the smoke briefs), the target case already exists as a
  CaseSpec in `eval_interactive/case_specs/smoke/`; Track A may
  promote it or reference it as the target. For the manual-probe
  brief, the target case must be authored.
- **Neighbor cases.** At least 2 per family, drawn from cases that
  share the failure shape or the relevant UC but should pass under
  the brief's expected behaviour. These exercise the same surface
  without re-triggering the same failure.
- **Negative-control cases.** At least 2 per family. Cases that
  should **not** trigger the brief's failure shape — designed so
  that a remediation that fixes the target without breaking the
  negatives is provably narrow.
- **Shadow cases.** At least 2 per family. Held out from the dev
  agent; visible only to the human and to the review agent.

**Shadow-split mechanism.** The shadow-split mechanism must enforce
the §5.1 contract: the dev agent cannot consume shadow-class cases
during development; the human and the review agent can. The dev
agent's Context Pack must propose the mechanism shape (file layout,
naming convention, runner flag, access-control documentation) and
the human reviews before implementation. A repo grep for "shadow"
shows only Wave A6.1 shadow-audit references in
`llm_persona_reviewer.py`; the §5.1 sprint-acceptance shadow split
is not currently delivered. Track A delivers the v0 of that
mechanism.

**Track A explicitly excludes runtime / prompt / judge / corpus
changes.** Track A is content-authoring on the eval surface plus a
new shadow-split harness file. It **does not fix** any failure that
the case families catalogue. Remediation is a separate sprint per
case family.

**Cs_192 reminder.** If Track A authoring on the cs_192 family
surfaces a `CONTRACT_VIOLATION:active_use_case`-shaped target case
that would re-open Sprint 8 §K0 territory, the dev agent flags
`human_review_required` per §3.2 Q2 and does not invent a Tier-0
invariant. The cs_192 family is authored; remediation is out of
scope.

### Track B — `R-prompt-projection-already-called-soft-signal`

**Diagnostic-slot scope.** Add an `already_called` slot to the
per-step projection emitted by `ContextProjectionBuilder.java`
(file path the dev agent confirms in its Context Pack). The slot
shape:

```
already_called: [
  {
    "tool": "<tool name>",
    "arguments_hash": "<stable hash of normalized args>",
    "at_step": <prior step index>
  },
  ...
]
```

The slot lists prior identical-args tool calls within the same
`AgentRunLoop.run(...)` invocation. The slot **does not enforce**
anything; the LLM owns whether to re-emit. The slot is read by the
LLM as a diagnostic signal.

**Regression test.** A new Java regression test demonstrates: (a)
the slot is populated when a prior identical-args call exists; (b)
the slot is empty / absent when no prior call exists; (c) the
runtime does **not** short-circuit on the slot being populated
(write tools opt out; identical-args re-emission is still dispatched
unless the LLM itself avoids it). A future
`R-idempotent-read-tool-short-circuit` is the conditional follow-on
if Track B's soft-signal-only approach proves insufficient.

**Track B layer = `prompt_projection`.** Pre-investigation. Track B
is a single layer, not multi-layer; it is bundled per the Bundle-or-
defer policy below. No keyword / regex / if-else / enum / per-UC
matrix is added.

## Bundle-or-defer policy (Track B only; Track A is content
authoring)

Track A is content authoring and is not subject to bundling: the
deliverable IS the data. Bundle-or-defer applies only to Track B.

Track B's soft-signal slot is the canonical `prompt_projection`
bundle shape per Sprint 19's §"Bundle-or-defer policy": the slot
addition is reversible, projection-only, no keyword / regex / if-
else / enum / per-UC matrix, and carries a regression test. Bundle
is allowed.

**Defer (do not bundle in Track B):**

- Any short-circuit logic on the `already_called` slot — that is
  `R-idempotent-read-tool-short-circuit` and is `infra` layer, not
  `prompt_projection`. Sprint 20 Track B is read-side only.
- Any prompt rewrite that changes how the LLM responds to the slot
  — Track B adds the slot; prompt copy that exhorts the LLM to
  consume it is a separate prompt sprint and out of Sprint 20 scope.
- Any change to `AgentRunLoop` write-tool dispatch — the future
  HandoverOrchestrator owns write-tool idempotency; Track B does
  not touch write tools.

## Layer-classification + anti-hardcode stanza

**Target failure layer:** **multi-layer prospective** per the
`feedback_multi_layer_prospective_stanza` precedent (Sprint 19
established this shape for two-track sprints with different layers
per track). Track A lands on `eval_spec` (case-family authoring on
the eval surface, building the §5.1 generalization-coverage
artefact for downstream sprints) and `infra` (the shadow-split
mechanism is an eval-harness addition). Track B lands on
`prompt_projection`. The dev agent does **not** walk §3.2 per case
to choose Track A's layer because Track A is not a failure
diagnosis sprint; the per-case §3.2 walk applies to remediation
sprints, not authoring sprints.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.
`iteration_governance.md` §3.2 Q2 is the only branch that re-
classifies a finding as `java_guard`; per the Track A authoring
scope, no finding is being remediated, so no `java_guard` candidate
arises. Per the cs_192 reminder, an authoring-surfaced
`human_review_required` shape is flagged and stopped; no new
Tier-0 invariant is opened.

**Semantic hardcode:** No semantic hardcode introduced. Track A is
data authoring; the case-family content describes user shapes and
expected behaviours but does not encode keyword / regex / if-else
rules into runtime or prompt. The shadow-split mechanism is an
access-control + file-naming convention, not a semantic decision.
Track B's slot adds an observable projection field; the LLM owns
whether to consume it. No prompt if-else, no projection-time regex
branch, no per-UC matrix.

**Generalization coverage:** Track A **is** the generalization-
coverage deliverable for downstream sprints; coverage is therefore
the deliverable, not a check on a fix. Concrete bars: 10 case
families authored, each with ≥1 target + ≥2 neighbor + ≥2 negative
+ ≥2 shadow = 70+ CaseSpec-shaped entries (the dev agent confirms
the count in the Context Pack). Track B's coverage = (target) the
manual-probe trace + the slow-LLM cs_011 T2 shape; (neighbor) any
multi-tool turn from the smoke set that does not have a prior
identical-args call; (negative) ≥2 turns where the slot is absent
and re-emission would be correct; (shadow) deferred — Track B's
shadow set is built as part of Track A's authoring.

## Do not implement

- Any remediation of any G1 brief's underlying failure. Track A
  catalogues failures into case families; it does not fix them.
  G3+ runtime sprints, gated by Track A's families, fix them.
- Any change to `eval_interactive/case_specs/smoke/` CaseSpecs
  beyond promoting existing target cases into the new family
  structure (read-only references are allowed; behaviour-altering
  edits are not).
- Any change to `eval_interactive/case_spec_overrides.yaml`. L3
  overrides go through Wave A5 / A6 review, not this sprint.
- Any change to `eval_interactive/personas*.yaml`. Persona edits
  are out of scope.
- Any FAQ corpus edit (`data/faq/*`,
  `FAQ-knowledge_include_help_url.csv`).
- Any judge rubric edit (L1 / L2 / L3 weights, thresholds, or
  language).
- Any prompt edit beyond Track B's projection slot. **No new prompt
  paragraph, no prompt if-else, no `system_prompt.txt` copy edit.**
  Track B is a projection-assembly addition; the prompt template
  consumes the slot if and only if the prompt already iterates the
  projection.
- Any keyword / regex / if-else / enum / per-UC matrix added to
  runtime or prompt for a semantic decision.
- Any new Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md`.
  cs_192-shaped findings flag `human_review_required`.
- Any short-circuit logic on the `already_called` slot. That is
  `R-idempotent-read-tool-short-circuit`, conditional follow-on,
  out of Sprint 20 scope.
- Any work on the 16+ R-items in `docs/action_bank.md` §5.2 not
  named in this objective. The Wave A5 / A6 L3 review batch, the
  slow-LLM placeholder coalesce, the UC-K case_id binding, the
  duplicated-greeting projection fix, the faqMissCount threshold,
  the per-case trace dump, the sprint-narrative reconciliation,
  and the promoted phase-plan-directive-followship soft signal are
  all explicitly out of scope.
- Any edit to `docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`,
  `docs/foundational/*`, `docs/runtime_freeze_and_risk_policy.md`,
  or any sprint archive under `docs/sprints/`. The Sprint 20
  archive copies are written by the deliver agent at sprint close.
- Any architecture-health metric collection. Per §6, the four
  metrics remain `collection_status: not_started`.
- Any change to `docs/codex-findings.md` by the dev agent. The
  Codex review writes that file at sprint close.

## Success metrics (per `iteration_governance.md` §5.1)

- **Target Track A deliverable.** 10 case families authored (1 per
  G1 brief), each meeting the ≥1 target + ≥2 neighbor + ≥2
  negative + ≥2 shadow shape. The shadow-split mechanism is
  delivered with a documented enforcement story (file naming,
  runner flag, access boundary).
- **Target Track B deliverable.** `already_called` slot lands in
  projection assembly with the shape spec above. Regression test
  demonstrates the three behaviour bars (populated when prior call
  exists, empty otherwise, runtime does not enforce).
- **Neighbor cases no regression.** Smoke rerun after Track B's
  bundled fix shows no regression on the 8 non-regressed smoke
  cases from Sprint 19. Track A authoring does not run the smoke;
  Track B does.
- **Negative-control no false positive.** Track B's named
  negatives (≥2 turns where the slot is absent and re-emission
  would be correct) confirm the slot does not over-populate.
- **Shadow no regression.** Track A delivers the shadow split;
  Sprint 20 itself does not consume the shadow set. The first
  sprint to consume shadow is a downstream remediation sprint.
- **Safety floor unchanged.** Tier-0 safety invariants (PII,
  safety, identity verification, imminent harm) remain green.
- **Grounding floor unchanged.** FAQ grounding diagnostics per
  `docs/current/faq_grounding_contract.md` remain at or above
  prior level.
- **Wrong-containment rate unchanged or down.**
- **Over-escalation rate unchanged or down.**
- **Architecture-health metrics not regressed.** §6 metrics remain
  `collection_status: not_started`.

## Review rule

Codex runs the Anti-Hardcode Review (`iteration_governance.md`
§4.1) on the Sprint 20 PR diff and writes its decision to
`docs/codex-findings.md` using the §4.2 sprint-close 4-line header.

Codex is told explicitly (in the review prompt at
`compact/sprint-020-review-prompt.md`) that:

- Track A's case-family authoring on the eval-spec surface is **not
  scope drift** — it is the sprint's deliverable, and the
  generalization-coverage table required by §4.1 Q8 should be
  satisfied by Track A's authoring, not by a Track A check.
- Track B's soft-signal slot in projection is **not scope drift**
  — it is the canonical `prompt_projection` bundle per Sprint 19's
  Bundle-or-defer policy and Sprint 20's Bundle-or-defer policy.
- The cs_192 `java_guard` reminder still applies: if any per-case
  Track A authoring surfaces a `CONTRACT_VIOLATION:active_use_case`
  target that would require a new Java-guard remediation without a
  current Tier-0, the dev agent flags `human_review_required` and
  does not invent a Tier-0; Codex confirms.
- Out-of-scope concerns (e.g. opinions on the other R-items, on
  G3+ remediation order, on the shadow-split's enforcement
  strength beyond v0) are recorded in `docs/action_bank.md` as
  deferred items, not as blocking findings.

`fix_required` is reserved for concrete §4.1-failing findings on
the Sprint 20 diff: a bundle that introduces a forbidden hardcode,
Track B's slot without a regression test, a `human_review_required`
finding that was silently bundled, a case-family that encodes a
keyword / regex / per-UC matrix into the CaseSpec content, a
shadow-split mechanism that fails to enforce the §5.1 contract
(dev-agent-readable shadow files), a missing case-family family
for any of the 10 G1 briefs.

## Deliverables (what the dev-agent PR must contain)

- A single PR on a fresh branch (recommended:
  `sprint-020-g2-and-already-called` or similar).
- Track A case families under `eval_interactive/case_specs/`
  (exact sub-directory the dev agent proposes in the Context Pack).
- Track A shadow-split harness (exact location proposed in the
  Context Pack; documented in the handoff).
- Track B `already_called` projection-slot wiring in
  `ContextProjectionBuilder.java` (path confirmed) + regression
  test under `server/test/**`.
- A 12-section Sprint 20 handoff at
  `docs/sprints/sprint-020-handoff.md` following the Sprint 17 /
  Sprint 18 / Sprint 19 precedent.
- Updated `docs/10-handoff.md` leading with Sprint 20.
- No edit to `docs/sprint_objective.md`, `docs/sprints/*`, or any
  archived file. The deliver agent archives at sprint close.

Codex writes its review to `docs/codex-findings.md` after the
commits land on `design-v1-without-human-review`, per the review
prompt at `compact/sprint-020-review-prompt.md`.

---

## Sprint 20 fix iteration

Date appended: 2026-05-13

### Status

Sprint 20 closed at Codex review with `decision: fix_required`,
`blocking_count: 2`. This fix iteration narrowly addresses those two
blocking findings on the same branch `design-v1-without-human-review`.
The parent Sprint 20 objective (everything above this `---` divider)
remains the authoritative scope statement for everything Sprint 20
ships; this fix iteration adds nothing new — it closes two evidence
gaps named by Codex against bars the parent objective already
asserts. **No new sprint id is opened.** The artefacts at sprint close
remain `docs/sprints/sprint-020-objective.md` (this file) and
`docs/sprints/sprint-020-handoff.md` (parent), with a fix-iteration
addendum at `docs/sprints/sprint-020-fix-handoff.md` (NEW, this
iteration).

### Sources

- `docs/codex-findings.md` — top of file, `Sprint Review Decision`
  block + Finding 1 at lines 6–54 + Finding 2 at lines 56–85.
- Parent Sprint 20 objective bars satisfied by the fix (this file,
  above the divider):
  - **Track B "runtime does not short-circuit"** — line 160–161 of
    this file: "the runtime does **not** short-circuit on the slot
    being populated (write tools opt out; identical-args re-emission
    is still dispatched unless the LLM itself avoids it)".
  - **Track B target coverage including cs_011 T2** — lines 231–233
    of this file: "Track B's coverage = (target) the manual-probe
    trace + the slow-LLM cs_011 T2 shape; (neighbor) any multi-tool
    turn from the smoke set ...".
  - **Track A shadow-split enforcement story consistency** — the
    parent objective's Track A scope at lines 110–119 names "file
    layout, naming convention, runner flag, access-control
    documentation" as the shadow-split mechanism proposal surface;
    the v0 mechanism that actually shipped relies on directory
    boundary + custom-path-only loading + documented self-restraint
    (see `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` and
    `docs/sprints/sprint-020-handoff.md` §11 Q1). The fix reconciles
    `_ACCESS_BOUNDARY.md` with what actually shipped — it does not
    add the runner-flag gate.

### Implement only

**Finding 1 fix — runtime non-enforcement + cs_011 T2 target
coverage.** Add **one** new integration test under
`server/src/test/java/com/gumtree/csagent/integration/` that drives
`AgentRunLoopImpl.run(...)` end-to-end with two identical LLM-emitted
tool calls and verifies (a) both calls reach
`ToolDispatcher.dispatch(...)`, (b) the second-step projection
produced by `ContextProjectionBuilder.build(...)` contains an
`already_called` entry whose `tool` + `arguments_hash` reference the
first call. The test demonstrates the "runtime does not short-circuit"
bar at the runtime level (the existing
`AlreadyCalledProjectionTest.java` only demonstrates the bar by hand-
constructing `ToolEvent`s; Codex Finding 1 requires the bar be
demonstrated at `AgentRunLoopImpl.run` granularity, not at
`ContextProjectionBuilder` granularity alone).

Plus a separate test (the dev agent decides unit-shape under
`server/src/test/java/com/gumtree/csagent/service/runtime/` OR
integration-shape under `server/src/test/java/com/gumtree/csagent/integration/`,
and cites the choice in the fix handoff with a one-line justification)
that exercises the **cs_011 T2 shape** — a multi-tool turn where the
projection would surface a prior identical-args call that the LLM (or
the planner) is expected to weigh before re-emitting. The cs_011 T2
shape is described in `docs/action_bank.md:464` and the cs_011
failure brief at `docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md`.
The test does NOT remediate cs_011's underlying failure — it only
demonstrates the slot is populated for the cs_011-shape input under
the `already_called` contract.

**Finding 2 fix — shadow access-boundary reconciliation, Option A.**
Edit `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`:

1. **Remove** the `Eval-harness runner (with --include-shadow)` row
   from the enforcement table — currently line 23 of that file.
2. **Move** the `--include-shadow` flag description out of the
   `Enforcement (v0)` section item #2 (currently lines 44–48 of that
   file) into a new "Known v0 gaps / v1 hardening" section. The
   relocated text describes the flag as a documented v1 hardening
   direction the next eval-governance sprint may add, not as a
   currently-enforced gate.
3. **Reconcile** the `Enforcement (v0)` section so it lists only the
   v0 mechanism that actually shipped: (i) directory boundary, (ii)
   custom-path-only loading via `CaseSetManager.load_custom(path)`,
   (iii) documented self-restraint. The reconciled section should
   match the actual v0 shape described in
   `docs/sprints/sprint-020-handoff.md:443–448`.

The fix preserves the existing `Known v0 gaps` content; it merely
moves the runner-flag bullet into a refreshed/renamed section
("Known v0 gaps / v1 hardening") that also retains the existing v1
hardening reference.

### Do not implement

- **Do not implement the `--include-shadow` CLI flag.** That is the
  `R-shadow-include-flag-runner-gate` R-item (`infra` / eval harness,
  per `docs/sprints/sprint-020-handoff.md:927`) and is explicitly
  separate scope. The fix is documentation-only on the access boundary.
- **Do not add runtime short-circuit logic** on the `already_called`
  slot. That is `R-idempotent-read-tool-short-circuit` (`infra`,
  conditional follow-on) and remains out of scope per the parent
  objective `Do not implement` list. The fix only adds evidence at
  the runtime layer that the slot is **not** consulted for dispatch
  decisions; it does not change dispatch behaviour.
- **Do not edit any CaseSpec** (visible or shadow) and do not author
  any new CaseSpec. The cs_011 T2 shape test uses existing fixtures
  (or a hand-constructed `BotSession` / `ToolCall` pair in the test
  itself, matching the pattern of existing integration tests like
  `AgentRunLoopAd1002IntegrationTest.java`).
- **Do not edit any prompt file** (`system_prompt.txt`, any
  prompt-template asset). The cs_011 T2 follow-on for prompt
  consumption is `R-already-called-prompt-consumption` per
  `docs/sprints/sprint-020-handoff.md:910`, which is separate scope.
- **Do not edit Sprint 20 handoff** at
  `docs/sprints/sprint-020-handoff.md` — it is the closed Sprint 20
  archive. The fix writes its own
  `docs/sprints/sprint-020-fix-handoff.md`.
- **Do not edit `docs/action_bank.md`** during the fix. Action-bank
  deltas land at *final* close after Codex re-review passes (the
  deliver agent applies them).
- **Do not edit `docs/codex-findings.md`** during the dev fix. Codex
  re-writes the `Sprint Review Decision` header in place on re-review.
- **Do not open any new R-item.** If something out-of-scope surfaces
  during the fix, the dev agent records it in the fix handoff under
  "Out-of-scope observations" and stops — the deliver agent triages
  at final close.

### Target cases / regression coverage

- **Finding 1 target:** the new integration test (and the cs_011 T2
  shape test) — runtime-level evidence for the slot's
  non-enforcement bar plus cs_011 T2 target coverage that the parent
  objective lines 231–233 already claim.
- **Finding 2 target:** `_ACCESS_BOUNDARY.md` internal consistency
  with `sprint-020-handoff.md:443–448`. No code paths affected.
- **Neighbor coverage:** the full server test suite must remain
  green. Run the canonical command (see the dev prompt at
  `compact/sprint-020-fix-dev-prompt.md` for the verified runner)
  and quote the tail in the fix handoff.
- **Negative / shadow:** unchanged from parent objective. The fix
  does not consume any new shadow set; the shadow-split mechanism v0
  remains as-shipped after the doc reconciliation.

### Success metrics (per `iteration_governance.md` §5.1)

- **Target Finding 1.** The new integration test asserts (a) both
  calls reach `ToolDispatcher.dispatch(...)`, (b) the second-step
  projection contains an `already_called` entry referencing the
  first call. The cs_011 T2 shape test asserts the slot is populated
  for the cs_011-shape input.
- **Target Finding 2.** `_ACCESS_BOUNDARY.md` no longer claims an
  unimplemented `--include-shadow` gate as an active enforcement
  mechanism. Its `Enforcement (v0)` section enumerates only the v0
  mechanism actually shipped.
- **Neighbor cases no regression.** Full server test suite green.
- **Safety floor unchanged.** No code touched in
  `ControlKernel.java`, `ToolPolicyEnforcer.java`, the PII redaction
  path, or any identity-verification surface.
- **Grounding floor unchanged.** No FAQ corpus edit, no grounding
  rubric edit.
- **Architecture-health metrics not regressed.** §6 metrics remain
  `collection_status: not_started`. The fix does not introduce a
  new semantic hardcode.

### Review rule

Codex re-reviews the fix bundle only (not the full Sprint 20 PR) per
the review prompt at `compact/sprint-020-fix-review-prompt.md`. The
re-review verifies the two findings are resolved without scope
expansion and replaces the existing `Sprint Review Decision` header
at the top of `docs/codex-findings.md` in place. New header text:
"## Sprint Review Decision (Sprint 20 fix re-review)" with the same
three fields (`decision`, `blocking_count`, `summary`).

### Layer-classification + anti-hardcode stanza (fix-iteration form)

**Target failure layer:** **two-finding multi-layer**, fix iteration.
Finding 1 fix lands on `prompt_projection` (regression-test
extension at the `AgentRunLoopImpl.run(...)` runtime boundary,
demonstrating the slot's projection-only nature; no
projection-assembly logic changes, only test evidence at a different
layer of the stack). Finding 2 fix lands on `infra` (eval-harness
documentation reconciliation, not a code path; the v0 mechanism
shipped is unchanged). The fix is iterating on Sprint 20's already-
delivered work and is not a new sprint; the per-case §3.2 walk does
not apply (no new failure is being diagnosed).

**Tier-0 invariant:** This fix iteration adds no Tier-0 invariant.
`iteration_governance.md` §3.2 Q2 governs Tier-0; the fix touches
neither the `java_guard` layer nor proposes a new invariant in
`docs/runtime_freeze_and_risk_policy.md`. If the dev agent
discovers during Finding 1 fix that an unconditional
`java_guard`-shape short-circuit exists in `AgentRunLoopImpl.run(...)`
(contradicting the parent objective's non-enforcement claim), the
dev agent flags `human_review_required` and stops; this is the
parent objective's cs_192 reminder pattern applied to the fix.

**Semantic hardcode:** No semantic hardcode introduced. Finding 1
fix adds **test evidence**, not a runtime branch — no new keyword,
no regex, no if-else, no enum extension, no per-UC matrix. The new
test asserts existing behaviour at a different granularity than
`AlreadyCalledProjectionTest.java`; it does not change runtime or
projection-assembly logic. Finding 2 fix is **documentation only**
on a non-code file; no semantic surface touched.

**Generalization coverage:** Coverage is the two Codex findings
themselves — target = the two findings; neighbor = full server test
suite (must stay green); negative = the unchanged parent Sprint 20
deliverables (Track A case families, Track B
`ContextProjectionBuilder` projection-assembly behaviour, the v0
shadow-split mechanism); shadow = unchanged (the fix does not
consume any shadow set; Sprint 20's shadow set was authored by
Track A and is not the subject of the fix). Per
`iteration_governance.md` §7.1 the four stanza fields are filled
without guessing intent: the fix iteration is narrowly two
findings, layer assignments are direct, and the generalization
coverage is bounded by the two findings.

### Deliverables (what the dev-agent fix PR must contain)

- New integration test for Finding 1 at
  `server/src/test/java/com/gumtree/csagent/integration/` (test
  filename the dev agent chooses; should signal Sprint 20 fix
  iteration + already-called runtime non-enforcement).
- Test for the cs_011 T2 shape (the dev agent decides unit-vs-
  integration shape; cites the choice in the fix handoff).
- Edit to `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`
  per Finding 2 above.
- New fix handoff at `docs/sprints/sprint-020-fix-handoff.md`
  (one section per finding — NOT a 12-section sprint handoff —
  with diff summary + test results + the parent Sprint 20 objective
  bar each fix satisfies + the full test-suite tail).
- **No** edit to `docs/sprints/sprint-020-handoff.md`,
  `docs/sprints/sprint-020-objective.md`,
  `docs/codex-findings.md`, `docs/action_bank.md`,
  `docs/sprint_objective.md` (this file — beyond what the deliver
  agent already appended), `docs/current/*`, `docs/foundational/*`,
  or any sprint archive under `docs/sprints/sprint-001-...020-*.md`.
