---
title: Iteration governance
doc_tier: current-runtime
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Always-loaded Layer-A constitution + the gates every role needs every
  session: §1 (LLM-first constitution), §2 (Failure Brief template),
  §3 (Fix Layer Classification checklist), §4.1/§4.2 (anti-hardcode
  kernel pointer + sprint-close header), §5.1-§5.5/§5.7 (Eval
  Acceptance Rules + smoke-demotion rule + mocked-LLM evidence gate),
  §7.1 (sprint-objective stanza template). On 2026-06-02 the high-churn
  role-specific process material was carved into on-demand Layer-B docs
  under docs/current/process/ (milestone-framework §8 + §4.3,
  prompt-artifact-rules §9, badcase-lifecycle §5.6 + §5.5 rationale,
  architecture-health-metrics §6) plus docs/current/governance-examples.md
  (the §2 and §7.2 worked examples). Moved sections leave a one-line stub
  here so §-number citations from archives still resolve. The Failure
  Brief and Fix Layer checklist drive G1 / G2.
---

# Iteration governance

This document is the always-loaded **Layer-A** core: the timeless
LLM-first **Constitution** (§1) plus the gates every role needs every
session — the Failure Brief template (§2), the Fix Layer Classification
checklist (§3), the Anti-Hardcode review kernel pointer + sprint-close
header (§4.1/§4.2), the Eval Acceptance Rules (§5, including the
smoke-demotion rule §5.5 and the mocked-LLM evidence gate §5.7), and
the required sprint-objective stanza template (§7.1).

High-churn, role-specific process material has been carved into
on-demand **Layer-B** process docs (load by role when relevant); the
numbers below stay as one-line stubs so §-number citations still
resolve:

- `process/milestone-framework.md` — §8 milestone framework + §4.3
  milestone-shared Codex review.
- `process/prompt-artifact-rules.md` — §9 agent prompt artifact rules.
- `process/badcase-lifecycle.md` — §5.6 curated bad-case suite
  lifecycle + the §5.5 dated rationale.
- `process/architecture-health-metrics.md` — §6 metric definitions.
- `governance-examples.md` — the §2 `cs_example_001` brief and the
  §7.2 worked sprint stanza.

Doc-tier and source-of-truth conventions are defined in
[`doc_governance.md`](doc_governance.md). Per-task reading lists and
the Context Pack Prompt are in
[`agent_context_guide.md`](agent_context_guide.md). This file
references both; it does not duplicate them.

**Governance-doc editing discipline**: planning-time scope authorization
(e.g., "if (a), fold back §X") does NOT authorize execution-time content.
Before editing any governance-tier doc, verify: (1) timelessness — no
sprint numbers, R-item IDs, or dates; (2) principle vs current-state —
governance teaches principles, not findings; (3) necessity — would
backlog carry the load without the edit? (4) durable shift vs reaction.
If any check fails, put the content in `action_bank.md` or sprint archives.

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

### Example brief — `cs_example_001`

Moved to [`governance-examples.md`](governance-examples.md) on 2026-06-02
to keep this file always-loadable. The worked six-field example
(hypothetical, not a real CaseSpec id) lives there; cite as
"governance-examples (§2 example)".

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

### 4.1 Nine-question anti-hardcode kernel

The canonical copy-pastable prompt lives at
[`docs/current/anti-hardcode-review-kernel.md`](anti-hardcode-review-kernel.md).
It contains nine questions, a scope exemption clause, and four
possible verdicts (`approve`, `approve with downgrade-to-signal
follow-up`, `reject as semantic hardcode`, `needs human architecture
decision`).

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

### 4.3 Milestone-shared Codex review

Moved to [`process/milestone-framework.md`](process/milestone-framework.md) on 2026-06-02 to keep this file always-loadable. Cite as "milestone-framework §4.3".

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

### 5.5 Smoke composite_score demoted to observation

The 14-case smoke summary metrics
(`mean_composite_score` / `mean_outcome_score` / `mean_judge_score` /
`task_success_rate` / `passed_cases`) are formally **demoted from
hard close gates to observations**. They continue to be computed,
recorded in `eval_interactive/results/*/results.json`, and tracked
across sprints, but they no longer block sprint or milestone close.
The dated rationale for this demotion is preserved in
[`process/badcase-lifecycle.md`](process/badcase-lifecycle.md) §5.5
(historical).

**What stays as hard close gate** (unchanged):

- **Codex §4.1 nine-question anti-hardcode kernel pass** at the
  PR / sprint / milestone level per the §4 dispatch convention.
- **Java test suite no new regression** (baseline preservation
  against the documented inherited-failure baseline).
- **Safety floor unchanged** — per §5.1.
- **Grounding floor unchanged** — per §5.1.

**Primary gate:** the curated bad-case suite manual review pass. The
suite lifecycle, tiering, and selection rules live in
[`process/badcase-lifecycle.md`](process/badcase-lifecycle.md) §5.6.

### 5.6 Curated bad-case suite (primary acceptance gate)

Moved to [`process/badcase-lifecycle.md`](process/badcase-lifecycle.md) on 2026-06-02 to keep this file always-loadable. Cite as "badcase-lifecycle §5.6" (incl. §5.6.1 / §5.6.2 / §5.6.3).

### 5.7 Eval evidence gate (mocked-LLM)

**Eval evidence gate**: mocked-LLM tests cannot be primary evidence that
a prompt change caused a behaviour change — the mock controls the measured
variable. Real-LLM rerun is the eval evidence gate; mocked-LLM tests
cover projection/rendering/dispatch wiring only.

## 6. Architecture-Health Metrics

Moved to [`process/architecture-health-metrics.md`](process/architecture-health-metrics.md) on 2026-06-02 to keep this file always-loadable. Cite as "architecture-health-metrics §6". The four metrics are defined only; collection is not implemented.

## 7. Required sprint-objective stanza for semantic-touching sprints

A **semantic-touching sprint** is any sprint that changes prompt, a
runtime semantic decision (UC routing, drift detection, escalation
posture, follow-up policy), the eval spec, or judge calibration.
Pure infra, docs-only, config-governance, and characterization-test
sprints are **exempt** and need not include the stanza.

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

### 7.2 Worked example — hypothetical Sprint 18 fix

Moved to [`governance-examples.md`](governance-examples.md) on 2026-06-02
to keep this file always-loadable. The filled-in stanza example for
`cs_example_001` lives there; cite as "governance-examples (§7.2 example)".

**Multi-layer prospective variant**: investigation + bundle-or-defer
sprints span multiple candidate layers; §7 stanza is per-decision-outcome
multi-layer prospective (enumerate possible §3.2 layers per case). Bundle
policy must live in sprint_objective so Codex can verify scope discipline.

## 8. Milestone framework

Moved to [`process/milestone-framework.md`](process/milestone-framework.md) on 2026-06-02 to keep this file always-loadable. Cite as "milestone-framework §8.N" (§8.1–§8.7).

## 9. Agent prompt artifact rules

Moved to [`process/prompt-artifact-rules.md`](process/prompt-artifact-rules.md) on 2026-06-02 to keep this file always-loadable. Cite as "prompt-artifact-rules §9.N" (§9.1–§9.6).
