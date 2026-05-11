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
