---
title: Iteration governance
doc_tier: current-runtime
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-11
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Iteration constitution + governance bundle. Section 1 is the LLM-first
  constitution. Sections 2-6 are the operational gates: Failure Brief
  template, Fix Layer Classification checklist, Anti-Hardcode review
  prompt, Eval Acceptance Rules, and Architecture-Health Metric
  definitions. Metrics are defined only; collection lands in a later
  sprint. The Failure Brief and Fix Layer checklist drive G1 / G2.
---

# Iteration governance

This document is the operational rulebook for how we iterate on the
customer-service agent. Section 1 is the LLM-first **Constitution**.
Sections 2–6 are the operational gates that turn the Constitution into
day-to-day decisions: a **Failure Brief Template** for capturing what
went wrong, a **Fix Layer Classification Checklist** for routing the
fix to the right layer, an **Anti-Hardcode Review Prompt** for catching
semantic hardcodes in PR review, **Eval Acceptance Rules** for what
counts as a passing sprint, and **Architecture-Health Metric
definitions** for tracking whether the system is getting healthier or
more brittle over time. Section 7 specifies the **sprint-objective
stanza** that semantic-touching sprints must include.

Doc-tier and source-of-truth conventions are defined in
[`doc_governance.md`](doc_governance.md). Per-task reading lists and
the Context Pack Prompt are in
[`agent_context_guide.md`](agent_context_guide.md). This file
references both; it does not duplicate them.

## 1. Constitution

### 1.1 Objective

Build a customer service agent that solves user problems with LLM-first
semantic flexibility, not a keyword chatbot.

### 1.2 Primary principle

Rules define boundaries. LLM owns semantic understanding.

### 1.3 LLM owns

- user goal
- issue relation
- use case hypothesis
- drift / topic shift
- next action
- escalation posture
- response strategy
- natural customer-facing wording

### 1.4 Runtime owns

- tool schema
- capability / permission boundary
- PII and safety floor
- grounding floor for factual claims
- budget / timeout
- idempotency
- persistence
- trace and eval contract

### 1.5 Iteration rule

Do not fix semantic failures by adding keyword / regex / if-else /
enum expansion unless a Tier-0 invariant is broken.

### 1.6 Evaluation rule

Eval is evidence, not authority. A pass-rate increase is insufficient
unless it improves generalizable customer problem-solving and does not
regress safety, grounding, wrong containment, or architecture health.

### 1.7 Forbidden

- encoding raw eval phrases into Java or prompt
- adding UC-specific hard rules for soft semantic decisions
- widening eval spec to accept a genuine bot mistake
- optimizing visible eval at the cost of shadow/generalization
- using prompt as an if-else rule dump

## 2. Failure Brief Template

A **Failure Brief** is a short, structured record of one observed agent
failure. Briefs are filed jointly by a human (who labels the expected
behaviour) and a deliver agent (who labels the layer hypothesis and the
"do not do" list). They live under
`docs/diagnostics/failure-briefs/<brief-id>.md` once the directory is
populated in Sprint 18 (G1). Briefs are the input to G1 / G2: they
become the source for Failure Portfolio entries and, downstream, the
target / neighbor / negative / shadow case families.

Every brief has these six fields:

- **What happened?** — the observed bot behaviour in one or two
  sentences, written so a reader who has never seen the trace can
  understand the failure shape. *Why this field exists: the bug starts
  with a concrete observation, not a hypothesis.*
- **What should a good CS agent have done?** — the expected behaviour
  on the same input, written from the customer's perspective. *Why
  this field exists: pins the failure to a contrast — bot did X,
  should have done Y — so the gap is named, not implied.*
- **Why does this matter?** — the user / business / safety impact in
  one line; references the Constitution clause the failure violates
  if applicable. *Why this field exists: forces a relevance check.
  A failure with no plausible user impact is a candidate for
  `eval_spec` reclassification, not a runtime fix.*
- **Is this a one-off or a pattern?** — one of `one-off`, `pattern`,
  or `unknown`, with a one-line note on evidence (e.g. number of
  similar traces, neighboring CaseSpec ids, prior sprint references).
  *Why this field exists: pattern failures justify a case family;
  one-off failures usually do not justify a runtime change.*
- **Which layer is likely responsible?** — one of the layers in
  Section 3, with a one-line justification. Multiple-candidate
  hypotheses are allowed; Section 3's checklist disambiguates.
  *Why this field exists: forces the layer hypothesis to be explicit
  and disprovable, instead of defaulting to "fix Java".*
- **What should NOT be done?** — the tempting-but-wrong fix
  (typically a keyword / regex / if-else / enum / per-UC matrix) and
  the reason it is wrong (usually a Constitution clause). *Why this
  field exists: every brief encodes a guardrail against the
  short-term hardcode that would close the symptom without solving
  the failure.*

### Example brief — `cs_example_001` (hypothetical; not a real CaseSpec id)

**What happened?**
A UC-A user (listing visibility / paid Top Ad) says on turn 2 "and BTW
the replies to my buyer aren't coming through either, can you check?"
The bot continues to stamp `active_use_case=UC-A` and answers the
messaging complaint as if it were a follow-up about the listing.

**What should a good CS agent have done?**
Recognize the topic shift on turn 2, treat the messaging complaint as
a new issue, and reroute to UC-C (Replies / Messaging) — or, if
ambiguous, ask one clarifying question — instead of carrying UC-A
forward.

**Why does this matter?**
Carrying the prior UC through a topic shift corrupts intake, gives an
off-topic answer, and is exactly the failure mode the Constitution's
drift / topic-shift bullet (§1.3) says the LLM is supposed to own.

**Is this a one-off or a pattern?**
Pattern. UC-A↔UC-C soft-shift is a documented cross-UC scenario
already partly covered by the Sprint 10 §L1 reroute pipeline. The
remaining question is whether the projection / signals reaching the
LLM are sufficient on a soft shift.

**Which layer is likely responsible?**
Most likely `prompt_projection` (the LLM did not see a sufficient
alternate-UC signal on turn 2) or `semantic_planner` (the LLM saw the
signal and still chose UC-A). Section 3's checklist disambiguates.

**What should NOT be done?**
Adding a regex on "replies" / "messages not coming through" to
`DriftDetector` to force the reroute. That is a keyword / regex
semantic hardcode and breaks the Constitution's Iteration rule
(§1.5). The fix lives in `prompt_projection` (surface a soft
alternate-UC signal) or `semantic_planner`, not in Java pattern
matching.

## 3. Fix Layer Classification Checklist

Use this checklist any time a failure is observed and a fix is being
considered, **before any code is written**. The checklist routes the
fix to one of nine layers. Walk the questions in order; the first
matching question wins. The point is not to find the "best" layer in
the abstract — it is to prevent every failure defaulting to a Java
guard.

### 3.1 Layer set

- `infra` — orchestration, transport, persistence, timeouts, OOM,
  endpoint / credential / config wiring. Owns the run loop not
  crashing.
- `java_guard` — deterministic kernel-level invariants the Runtime
  must guarantee (§1.4). Adding one requires a current Tier-0
  invariant in [`../runtime_freeze_and_risk_policy.md`](../runtime_freeze_and_risk_policy.md)
  §1 / §2.
- `prompt_projection` — what state, signals, candidate lists, and
  diagnostics are surfaced to the LLM in the per-turn projection.
  Owns whether the LLM has the inputs to make a correct semantic
  choice.
- `skill_state` — multi-tool / multi-turn flow state: entity context,
  task status, intake fields, drift carry-over, same-UC continuity.
  Owns the durability of state across turns.
- `semantic_planner` — the LLM's own semantic choices (UC hypothesis,
  next action, escalation posture, follow-up). Owned by §1.3.
- `eval_spec` — the CaseSpec, the expected-behaviour rubric, the
  judge configuration. Owns whether the eval is asking the system
  to do something it can and should do.
- `product_policy` — whether the underlying ask is a product /
  policy decision the bot cannot make alone (e.g. "may the bot
  share an advert URL?", "is this refund liability allowed?").
- `judge_calibration` — the judge's own stability / rubric quality;
  flips on the same prompt + CaseSpec across reruns.
- `human_review_required` — no clean classification, or the failure
  looks like Java-guard territory but no current Tier-0 invariant
  covers it. Escalate; do not invent a new Tier-0.

### 3.2 Decision questions (first match wins)

1. Is the session failing to start, crash on infra, or hit a timeout
   / OOM not caused by tool semantics? → `infra`.
2. Is a current Tier-0 invariant named in
   [`../runtime_freeze_and_risk_policy.md`](../runtime_freeze_and_risk_policy.md)
   §1 / §2 being broken? → `java_guard`. If the failure *looks like*
   Java-guard territory but no current Tier-0 invariant covers it,
   flag `human_review_required` rather than inventing a new Tier-0
   invariant.
3. Did the LLM choose validly within the available options, but the
   projection / context handed to it was wrong or impoverished
   (missing slot, missing candidate, missing diagnostic)? →
   `prompt_projection`.
4. Is a multi-tool / multi-turn flow losing state across turns
   (entity reference, task status, intake field, drift carry-over)?
   → `skill_state`.
5. Is the LLM choosing a semantically wrong action even when
   projection and state are correct (e.g. wrong UC hypothesis,
   unjustified escalation, missing follow-up, paraphrasing a
   `retrieved_but_unresolved` hit on a FAQ-path UC)? →
   `semantic_planner`.
6. Is the eval CaseSpec or judge asking the system to do something
   it cannot or should not do — a factual or policy impossibility,
   a frozen enum the CaseSpec asks to extend, a mis-rubric? →
   `eval_spec`.
7. Is the underlying ask a product / policy decision (e.g. "may the
   bot share an advert URL", "what is the bot's refund liability
   posture") that the runtime cannot adjudicate without product
   sign-off? → `product_policy`.

**Tail rule (judge stability):** if the same case flips across
reruns of the *same prompt and CaseSpec*, reclassify as
`judge_calibration` regardless of which question above otherwise
matched.

**Default tail:** if no question matches cleanly, →
`human_review_required`.

### 3.3 Why no Java guard by default

Most observed failures look like Java-guard territory because a
keyword / regex / if-else can paper over the symptom in one PR. The
Constitution's Iteration rule (§1.5) and the forbidden-list (§1.7)
explicitly rule this out for soft semantic decisions: those belong to
the LLM. A new Java guard is only justified when it protects a
current Tier-0 invariant. If no current Tier-0 covers it,
`human_review_required` is the correct exit — the human decides
whether to open a new Tier-0 or to push the fix back to
`prompt_projection` / `skill_state` / `semantic_planner`.

## 4. Review Agent Anti-Hardcode Prompt

The prompt below is handed to Codex (or any review agent) on any
change PR that touches a semantic surface — prompt, runtime semantic
decision, eval spec, judge calibration, or any new keyword / regex /
enum that influences a routing or escalation decision. Pure infra /
docs-only / config-governance / characterization-test PRs are
**exempt**: list the exemption explicitly in the verdict and return
`approve`.

The per-PR verdict set below is different from the **sprint-close
review header** used in `docs/codex-findings.md`. Both are spelled out
at the end of this section so the two are not conflated.

### 4.1 Copy-pastable prompt

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

### 4.2 Sprint-close review header (separate convention)

At sprint close, the review agent writes a sprint-level decision to
the top of `docs/codex-findings.md` using this 4-line header (same
convention Sprint 16 used):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

The per-PR verdict set in §4.1 and the sprint-close header in §4.2
are different artefacts. The per-PR verdict reviews a single PR; the
sprint-close header reviews the sprint as a whole and gates closure.

### 4.3 Milestone-shared Codex review (2026-05-16 update)

Per the §8 milestone framework introduced 2026-05-16: sub-sprints
within an active milestone may share a single Codex sprint-close
review at **milestone close** rather than dispatching Codex per
sub-sprint. The §4.1 nine-question kernel and the §4.2 sprint-close
header are written once per milestone, against the cumulative commit
range of all sub-sprints in that milestone.

**Per-sub-sprint Codex review remains REQUIRED** when the sub-sprint:

1. Introduces a new Tier-0 candidate (a candidate invariant for
   `docs/runtime_freeze_and_risk_policy.md` §1 / §2) — Codex must
   verify the candidate at sprint close before the next sub-sprint
   begins;
2. Crosses a §1.7 forbidden-list red line — Codex must verify the
   justification at sprint close;
3. Touches a hard-fenced surface that the milestone objective
   explicitly named out of scope (e.g., editing an existing case
   family per cascade fence);
4. Closes a sub-sprint with a `fix_required` outcome that needs
   per-sub-sprint re-review before the milestone can continue.

For default sub-sprints (semantic-touching but not Tier-0-adjacent,
not §1.7-adjacent, not hard-fence-violating, not fix-iteration on
prior sub-sprint), Codex is deferred to milestone close. The
deliver-agent surfaces the per-sub-sprint deferral choice in
`docs/milestone_objective.md` and the dev session records it in
each sub-sprint handoff §11 (the dev does NOT dispatch Codex
themselves; the deliver-agent + human dispatch at milestone close).

Sub-sprints exempted from the §7 stanza (pure infra, docs-only,
config-governance, characterization-test) remain Codex-exempt per
§4.1 exemption clause regardless of milestone framing.

## 5. Eval Acceptance Rules

A sprint or a PR is accepted only when its eval evidence clears every
bar below. This section operationalizes the Constitution's
Evaluation rule (§1.6): a visible pass-rate increase is not enough.

### 5.1 Acceptance bars

- **Target cases pass** — the cases the sprint or PR named as in
  scope must pass.
- **Neighbor cases no regression** — cases that share the failure
  shape or the relevant UC must not regress.
- **Negative-control cases unchanged** — cases designed to *not*
  trigger the new behaviour must not start triggering it (no new
  false positives).
- **Shadow cases no regression** — held-out cases not visible to the
  dev agent. Shadow results are readable only by the human / review
  agent; the dev agent may not consume them during development.
- **Safety floor unchanged** — Tier-0 safety invariants (PII, safety,
  identity verification, imminent harm) remain green.
- **Grounding floor unchanged** — FAQ grounding diagnostics per
  [`faq_grounding_contract.md`](faq_grounding_contract.md) (the six
  output classes, citation diagnostics) remain at or above their
  prior level.
- **Wrong-containment rate unchanged or down** — sessions that
  contain the conversation in the wrong UC, the wrong phase, or
  with no grounded answer must not increase.
- **Over-escalation rate unchanged or down** — `request_handover`
  rate on cases that should resolve must not increase.
- **Architecture-health metrics not regressed** — the Section 6
  metrics, where collected, must not regress.

### 5.2 Baseline pointer

The canonical baseline is [`../current_eval_baseline.md`](../current_eval_baseline.md).
Verify the date and the run reference before treating any specific
number as authoritative.

### 5.3 Visible-eval vs shadow

A visible-eval improvement that ships with a shadow regression is a
**fail**, not a pass. The Constitution's "do not optimize visible
eval at the cost of shadow/generalization" forbidden-list line
(§1.7) is enforced here.

### 5.4 No eval-side override of a real bug

An eval-side override — widening the CaseSpec to accept the bot's
actual output, relaxing the rubric, or downgrading a judge — may
**not** be used to mask a genuine bot mistake. The Constitution's
"widening eval spec to accept a genuine bot mistake" forbidden-list
line (§1.7) governs. If the bot's behaviour is wrong, fix the bot;
if the CaseSpec is wrong, fix the CaseSpec and document the override
in the sprint handoff with the layer classification
(`eval_spec`) from Section 3.

### 5.5 Smoke composite_score demoted to observation (2026-05-16 update)

The 14-case smoke summary metrics
(`mean_composite_score` / `mean_outcome_score` / `mean_judge_score` /
`task_success_rate` / `passed_cases`) are formally **demoted from
hard close gates to observations**. They continue to be computed,
recorded in `eval_interactive/results/*/results.json`, and tracked
across sprints, but they no longer block sprint or milestone close.

**Rationale:** the smoke composite_score has accumulated multiple
independent confounding sources that the deliver-agent + dev +
review pipeline has surfaced over Sprints 24-32:

1. **External LLM provider drift** (Sprint 31 §13 + Sprint 32
   Codex re-run evidence) produces +84% mean `elapsed_ms` widening
   and resulting contract-violation spikes with zero sprint-side
   code change.
2. **Judge calibration variance** — the same prompt + CaseSpec
   yields different judge scores across reruns on the same bot
   codebase (Sprint 25 §6.4 four-hypothesis walk).
3. **Mocked-LLM vs real-LLM gap** (Sprint 23 `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`) —
   smoke runs real LLMs but unit tests mock them; failure-shape
   variance between the two channels is observed.
4. **Eval rubric dimensions** — `mean_composite_score` is a
   weighted sum where the weights and the underlying dimensions
   have not been re-validated since Sprint 14 baseline; the
   weighting may be wrong for the post-Sprint-23 / post-Sprint-31
   prompt + projection surface.

Per Constitution §1.6 ("Eval is evidence, not authority"), a
metric that cannot be reliably attributed to sprint-side causes
cannot gate sprint close. Sprint 31 fix-iteration #1 already
operationally treated the smoke regression as OOSR via rigorous
disambiguation; this §5.5 formalizes the demotion.

**What stays as hard close gate** (unchanged):

- **Codex §4.1 nine-question anti-hardcode kernel pass** at the
  PR / sprint / milestone level per the §4 dispatch convention.
- **Java test suite no new regression** (baseline preservation; the
  inherited `SystemPromptUserRequestedTiebreakerTest` failure since
  Sprint 24-era working-tree mod is the documented baseline).
- **Safety floor unchanged** — Tier-0 safety invariants (PII,
  identity verification, imminent harm) remain green. Verified by
  the standard `hard_checks: no_pii_leakage / no_critical_policy_violation`
  schema on every CaseSpec.
- **Grounding floor unchanged** — the FAQ grounding diagnostics
  per `faq_grounding_contract.md` (six output classes, citation
  diagnostics) remain at or above their prior level. Verified per
  case at sprint close manual review of relevant traces.

**NEW primary gate (per §5.6 below):** curated bad-case suite
manual review pass.

**2026-05-21 update (Sprint 42 / M3-Eval S-Eval-1):** the outcome-only
`anchor_outcome` suite at `eval_interactive/case_specs/anchor_outcome/`
is the second human-judgment surface beside the curated bad-case suite
at `eval_interactive/case_specs/bad_cases/`; smoke remains
observation-only by design.

### 5.6 Curated bad-case suite as new primary acceptance gate

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
   programmatic gate.

**Important clarification (2026-05-17 refinement): the bad-case
suite is a human-reviewed gate, not an automated PASS/FAIL
programmatic gate.** Rationale:

- The judge dimensions and scoring weights in the eval harness are
  themselves unstable (see §5.5 confounding sources). Treating a
  programmatic `composite_score >= X` derived from the bad-case
  trace as a binary gate would re-import the same instability the
  smoke composite_score demotion (§5.5) was designed to escape.
- The `closure_criterion` field on each bad case is GUIDANCE for
  the human review — naming the observable end-states that count
  as "resolved". The human reads the trace, judges whether the
  bot's behaviour qualitatively matches the expected end-state,
  and decides PASS / FAIL / IMPROVING based on overall situation
  (not on a programmatic match of any single field).
- Early-stage (until the eval rubric is independently validated
  and stable), human review is the only reliable signal for
  whether the agent's behaviour is genuinely improving on the bad
  case shape. A milestone close requires the human's qualitative
  judgment, not a CI-style automated check.

**Sprint or milestone close decision** is made by the human (with
deliver-agent's recommendation) based on the per-case manual
review results PLUS the other §5.5 hard gates (Codex anti-hardcode,
Java tests, safety floor, grounding floor). FAIL on a bad case
does not auto-block close; it triggers a deliver-agent + human
conversation about whether the failure is in-scope for the closing
milestone or surfaces a new R-item for a future milestone.

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

## 6. Architecture-Health Metrics (definitions only)

These four metrics are defined here and are referenced by Section 5's
acceptance bars. Collection lands in a later sprint; no metric is
collected, dashboard'd, or alerted on as of Sprint 17.

| metric | definition | unit | observation cadence | source artifact | collection_status |
| --- | --- | --- | --- | --- | --- |
| `new_semantic_hardcode_count` | Number of new keyword / regex / if-else / enum entries added to runtime or prompt for a semantic decision in a PR | count per PR | per PR | PR diff + Anti-Hardcode review verdict | not_started |
| `soft_signal_conversion_count` | Number of existing semantic hardcodes downgraded to LLM-projected soft signals | count per sprint | per sprint close | sprint handoff | not_started |
| `planner_ownership_ratio` | Fraction of semantic decisions in the runtime owned by LLM planning vs Java guard / regex | percentage | per sprint close (manual count) | runtime survey | not_started |
| `shadow_disagreement_rate` | Fraction of shadow cases where LLM decision disagrees with the human-labelled expected behaviour | percentage | per shadow run | shadow eval result | not_started |

The direction of health is: `new_semantic_hardcode_count` down,
`soft_signal_conversion_count` up, `planner_ownership_ratio` up,
`shadow_disagreement_rate` down. When collection lands, Section 5.1's
"Architecture-health metrics not regressed" bar consults these.

## 7. Required sprint-objective stanza for semantic-touching sprints

A **semantic-touching sprint** is any sprint that changes prompt, a
runtime semantic decision (UC routing, drift detection, escalation
posture, follow-up policy), the eval spec, or judge calibration.
Pure infra, docs-only, config-governance, and characterization-test
sprints (Sprint 15 and Sprint 16 are recent examples) are **exempt**
and need not include the stanza.

Semantic-touching sprints **must** include the stanza below in
`docs/sprint_objective.md`. Codex review checks for it as part of the
scope check.

### 7.1 Stanza template

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** <one of: infra | java_guard |
prompt_projection | skill_state | semantic_planner | eval_spec |
product_policy | judge_calibration | human_review_required>

**Tier-0 invariant:** <"This sprint adds no Tier-0 invariant." OR
pointer to the Tier-0 invariant being protected in
`docs/runtime_freeze_and_risk_policy.md` §X.X>

**Semantic hardcode:** <"No semantic hardcode introduced." OR
"Introduces <named hardcode>; justification: <reason>; sunset plan:
<downgrade-to-signal trigger + target sprint id>">

**Generalization coverage:** <"target / neighbor / negative / shadow
case counts: <T>/<N>/<G>/<S>" OR "case family not yet built; deferred
to <case-family sprint id>">
```

Each field has one acceptable form. A sprint that cannot fill one of
the four fields without a stretch is a sprint that has not yet
decided what it is doing; it should be re-scoped before the dev
agent runs.

### 7.2 Worked example — hypothetical Sprint 18 fix for `cs_example_001`

The Section 2 brief for `cs_example_001` hypothesized that the
UC-A↔UC-C topic-shift failure lives in `prompt_projection`. A
hypothetical Sprint 18 takes the fix:

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
Constitution's drift / topic-shift bullet (§1.3 LLM owns) governs the
behaviour the projection enables; the projection itself sits inside
the Runtime's "trace and eval contract" responsibility (§1.4).

**Semantic hardcode:** No semantic hardcode introduced. The new
`alternate_candidate_use_cases` projected slot is a soft signal — a
list of UCs the existing `RuntimeIntentClassifier` already surfaces —
exposed to the LLM through the per-turn projection. The LLM owns
whether to act on it. No new keyword, regex, or per-UC matrix is
added to `DriftDetector` or the prompt.

**Generalization coverage:** target = UC-A↔UC-C drift (including
`cs_example_001` once promoted from a hypothetical brief).
Neighbor = UC-A↔UC-D, UC-A↔UC-F drift on the same projection.
Negative = UC-A single-issue follow-up that should stay in UC-A
(no false positive on the soft signal). Shadow = held-out UC-A↔UC-C
and UC-A↔UC-D drift traces, not visible to the dev agent. Counts
deferred to the G2 case-family sprint; this Sprint 18 stanza names
the required families.
```

A future deliver agent should be able to paste this template into a
new `docs/sprint_objective.md` and fill the four fields without
further interpretation. If filling a field requires guessing intent,
the sprint is not ready to start.

## 8. Milestone framework (2026-05-16 update)

This section introduces the **milestone framework** that groups
sub-sprints into architectural themes. The framework is additive on
top of the existing sprint + governance structure; it does NOT
replace sprints, the §7 stanza, or any §1 constitutional rule.

### 8.1 Definition

A **milestone** is a coordinated bundle of 3–5 sub-sprints sharing
a single architectural theme. Each milestone has:

- **One milestone objective document** at `docs/milestone_objective.md`
  (active milestone; archived to `docs/milestones/M<N>_objective.md`
  at milestone close).
- **One or more sub-sprint contracts** at `docs/sprint_objective.md`
  (active sub-sprint; archived to `docs/sprints/sprint-NNN-objective.md`
  at sub-sprint close per existing convention).
- **One milestone acceptance bar** derived from the curated
  bad-case suite (`eval_interactive/case_specs/bad_cases/`) per §5.6
  — typically a named bad case must close or improve materially.
- **Codex review at milestone close** per §4.3 (sub-sprints share
  one Codex review unless a per-sub-sprint trigger fires).

A **sub-sprint** within a milestone is a single dev-session unit of
work that ships a coherent slice of the milestone scope. Each
sub-sprint:

- Still has its own §7 stanza if semantic-touching.
- Still produces a `docs/sprints/sprint-NNN-handoff.md` dev-authored
  archive at sub-sprint close.
- Still flips relevant R-items in `docs/action_bank.md` per
  existing convention.
- Defers Codex review to milestone close per §4.3 default
  (unless a §4.3 per-sub-sprint trigger fires).

### 8.2 Why milestones (vs single-feature sprints)

The single-feature sprint cadence (Sprints 24-32) optimized for
narrow scope discipline at the cost of architectural throughput.
Sprint 24-32 each shipped 1 feature in ~1-2 days; the deliver-agent
+ Codex overhead per sprint averaged ~50% of dev time. For a
branch-context evolution (where the human accepts higher risk
tolerance for faster iteration), milestone-grained planning:

- Cuts deliver-agent overhead by ~50-70% (one milestone planning
  round + one milestone close vs three per-sprint rounds).
- Cuts Codex overhead by ~60-80% (one milestone Codex review vs
  three per-sprint Codex reviews per §4.3).
- Surfaces architectural coupling earlier (sub-sprints in the same
  milestone share design context, vs three independently-scoped
  sprints).
- Preserves §1.7 anti-hardcode discipline (each sub-sprint still
  fills the §7 stanza; Codex still verifies at milestone close).
- Preserves the Constitution and the LLM-first iteration rule
  (§1.5) — the milestone framework changes cadence, not
  architecture.

### 8.3 Milestone objective document schema

`docs/milestone_objective.md` carries:

```yaml
---
title: Milestone M<N> — <name>
doc_tier: current-runtime
status: current
implementation_status: not_started | partial | implemented
source_of_truth: this file
last_reviewed: <YYYY-MM-DD>
review_cadence: per milestone
notes: >
  free-form context (scope rationale, hard-fenced surfaces, why
  this is one milestone not split across two).
---
```

Body sections (analogous to `sprint_objective.md` shape):

1. **Milestone class** (semantic-touching layer breakdown across
   sub-sprints; §7 stanza coverage at the milestone level).
2. **Goal** — the architectural outcome the milestone targets,
   expressed as user-facing or bad-case-suite-anchored behaviour
   change (not as code paths).
3. **Sub-sprint sequence** — preliminary list of 3-5 sub-sprints
   with class, layer, scope (3 sentences each), and dependency
   relationships. Deliver-agent + human may refine at each sub-
   sprint planning round; the milestone objective is updated
   in-place.
4. **Non-goals** (explicit; what the milestone does NOT cover,
   including which Alice / bad-case dimensions are deferred to
   later milestones).
5. **Milestone acceptance bar** — one or more bad cases (per
   §5.6) that the milestone is expected to close or improve;
   per-bad-case closure criterion.
6. **Hard fences** at the milestone level (no edits to existing
   case families per cascade fence; no Tier-0 invention without
   human review; etc.).
7. **R-items consumed / surfaced** — which `action_bank.md`
   R-items the milestone is expected to consume; which new R-items
   the deliver-agent expects to surface.
8. **Codex review plan** per §4.3 — default milestone-shared OR
   per-sub-sprint triggers expected.
9. **Estimated milestone duration** (calendar weeks) — informational,
   not a gate.

### 8.4 Milestone close artefacts (deliver-agent owned)

At milestone close, the deliver-agent + human produce:

- Update `docs/milestone_objective.md` closure verdict (analogous
  to sprint handoff §12 — pass / fix_required / out-of-scope-review
  + classification + per-sub-sprint disposition).
- Archive the milestone objective to `docs/milestones/M<N>_objective.md`.
- Append a §6.5 "Closed milestone index" row to `docs/action_bank.md`
  (NEW subsection alongside the existing §6 closed-action index).
- Refresh `docs/10-handoff.md` §1 lead (demote current milestone
  to Preceding milestone; set next milestone or sub-sprint as
  Current).
- Reset `docs/sprint_objective.md` to the first sub-sprint of the
  next milestone (or to a planning placeholder if no next milestone
  is locked).
- Optionally start a new `docs/milestone_objective.md` for the next
  milestone.

The deliver-agent's existing close-out artefacts (per
`feedback_commit_at_end_bundles_deliver_artefacts.md`) move from
per-sprint to per-milestone cadence; per-sub-sprint dev handoff
files still ship per sub-sprint close.

### 8.5 When to break milestone framing

The framework is not mandatory. A single high-risk feature (e.g.,
the Single Handover Orchestrator P0 launch blocker per
`docs/release_gate.md` §1.1) may be its own "milestone of one
sub-sprint" if that better matches the scope discipline. The
deliver-agent + human decide at planning round.

A milestone that exceeds 5 sub-sprints is a signal that the
milestone scope is too large; the deliver-agent SHALL split it at
the next milestone planning round.

A sub-sprint that crosses an unrelated architectural surface is a
signal that the sub-sprint belongs to a different milestone; the
deliver-agent SHALL surface this at sub-sprint planning round
rather than smuggle the scope across milestones.

### 8.6 Sprint vs milestone vs R-item relationship

```
docs/action_bank.md  (backlog, cross-milestone persistent;
                     R-items flow in from research / bad cases /
                     sprint findings, flow out on close)
       ↓ (deliver-agent picks 3-5 related R-items into a milestone)
docs/milestone_objective.md  (current milestone north star;
                              names sub-sprints + acceptance bar;
                              archived to docs/milestones/M<N>_*.md
                              at close)
       ↓ (deliver-agent picks one sub-sprint contract from milestone)
docs/sprint_objective.md  (current sub-sprint dev/review contract;
                           archived to docs/sprints/sprint-NNN-objective.md
                           at sub-sprint close)
```

R-items are the persistent backlog. Milestones are the planning
horizon. Sub-sprints are the execution unit. The dev session
consumes the sub-sprint contract; the review session consumes
either the sub-sprint or the milestone (per §4.3); the deliver-
agent + human consume all three layers.

### 8.7 Backwards compatibility

Pre-2026-05-16 sprints (Sprint 1 through Sprint 32) were planned
under the single-feature cadence and do not retroactively become
milestones. They remain in `docs/sprints/sprint-NNN-*` archives
unchanged. The milestone framework applies prospectively from
Milestone M1 onward (2026-05-16+).

A sprint started without an explicit milestone (e.g., a
single-feature follow-on between milestones) is allowed; it
defaults to "milestone-of-one" framing per §8.5 and follows
existing per-sprint conventions for Codex review, deliver-agent
close-out, etc.
