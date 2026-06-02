---
title: <<PROJECT: project name>> — Constitution (Layer A)
doc_tier: current-runtime
status: current
implementation_status: <<PROJECT: implemented | partial | not_started>>
source_of_truth: this file
last_reviewed: <<PROJECT: YYYY-MM-DD>>
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Layer A — the always-loaded constitution. This is the ONLY governance
  file the project root (AGENTS.md) @-includes unconditionally, so keep it
  small (target <= ~20 KB). It holds the timeless rules; high-churn process
  mechanics live in Layer B (B-process/*, loaded on demand) and are
  referenced by the §-stubs below, never embedded here.
---

<!--
  ============================================================
  HOW TO INSTANTIATE THIS FILE
  ------------------------------------------------------------
  - Replace every `<<PROJECT: ...>>` slot with your project's value.
  - Lines with NO slot are PORTABLE — copy them unchanged. They encode
    the framework's invariant stance (LLM-first, anti-hardcode,
    eval-as-evidence) and only break if you intend a different stance.
  - `<!-- e.g. (csagent): ... -->` comments show the reference
    instantiation's value as a guide; delete them once filled.
  - The §-stubs (§4.3, §5.6, §6, §7.2, §8, §9) point at Layer B files.
    Keep them as pointers; do NOT inline Layer B content here.
  - Done when `grep -n '<<PROJECT:' constitution.template.md` is empty
    (in your instantiated copy).
  ============================================================
-->

# <<PROJECT: project name>> — Iteration Constitution (Layer A)

This document is the constitution: the timeless rules that shape every
change to the agent's behaviour. §1 is the core constitution; §2–§3 and
§5 are the always-loaded gates (Failure Brief, Fix-Layer classification,
Eval Acceptance); §7 is the sprint-objective stanza. Process mechanics
(milestone cadence, prompt-artifact rules, bad-case lifecycle, review
cadence, metrics) live in Layer B and are reached through the §-stubs.

**Role names** (dev / deliver / review / research) used below refer to the
framework's role registry in `../B-process/roles/`; rename them per
project if your team uses different labels.

**Governance-doc editing discipline** (PORTABLE — keep verbatim):
planning-time scope authorization (e.g., "if (a), fold back §X") does NOT
authorize execution-time content. Before editing any governance-tier doc,
verify: (1) timelessness — no sprint numbers, backlog-item IDs, or dates;
(2) principle vs current-state — governance teaches principles, not
findings; (3) necessity — would the backlog carry the load without the
edit? (4) durable shift vs reaction. If any check fails, put the content
in the backlog ledger or a sprint archive, not here.

## 1. Constitution

### 1.1 Objective

<<PROJECT: one or two sentences — what this agent is for and the core
quality bar that distinguishes it from a naive implementation.>>
<!-- e.g. (csagent): Build a customer service agent that solves user
     problems with LLM-first semantic flexibility, not a keyword chatbot. -->

### 1.2 Primary principle

<<PROJECT: state in one line the boundary between what rules/runtime
decide and what the model decides. This is the project's defining stance;
affirm it explicitly even if you adopt the framework default.>>
<!-- recommended default (any LLM-first project):
     "Rules define boundaries. The model owns semantic understanding."
     e.g. (csagent): adopts the default above verbatim. -->

### 1.3 The model (LLM) owns — the semantic / judgment layer

<<PROJECT: list the decisions the model owns — the semantic, contextual,
and judgment calls that must NOT be reduced to fixed rules.>>
<!-- e.g. (csagent): user goal; issue relation; use-case hypothesis;
     drift / topic shift; next action; escalation posture; response
     strategy; natural customer-facing wording. -->

### 1.4 The runtime owns — the deterministic layer

<<PROJECT: list what the deterministic runtime owns — the contracts the
model is NOT allowed to decide. Include at least: capability/permission
boundary, the safety/PII floor, budgets/timeouts, idempotency,
persistence, and the trace/eval contract.>>
<!-- e.g. (csagent): tool schema; capability/permission boundary; PII and
     safety floor; grounding floor for factual claims; budget/timeout;
     idempotency; persistence; trace and eval contract. -->

### 1.5 Iteration rule (PORTABLE)

Do not fix semantic failures by adding keyword / regex / if-else / enum
expansion unless an immutable runtime invariant (Tier-0) is broken. (The
project's Tier-0 / risk floor is defined outside this file; see
<<PROJECT: path to the Tier-0 / risk-policy doc>>.)
<!-- e.g. (csagent): docs/runtime_freeze_and_risk_policy.md §1/§2 -->

### 1.6 Evaluation rule (PORTABLE)

Eval is evidence, not authority. A pass-rate increase is insufficient
unless it improves generalizable problem-solving and does not regress the
project's protected dimensions: <<PROJECT: name the floors that must never
regress>>.
<!-- e.g. (csagent): safety, grounding, wrong containment, architecture
     health. -->

### 1.7 Forbidden

The pattern is portable: forbid the short-term hacks that close a symptom
while violating §1.2/§1.5. Keep the generic items; add project-specific
ones.

- encoding raw eval phrases into runtime code or the prompt
- adding case-specific hard rules for soft semantic decisions
- widening the eval spec to accept a genuine agent mistake
- optimizing visible eval at the cost of held-out generalization
- using the prompt as an if-else rule dump
- <<PROJECT: additional forbidden anti-patterns specific to your
  architecture, if any>>

## 2. Failure Brief Template (PORTABLE structure)

A **Failure Brief** is a short, structured record of one observed agent
failure, filed jointly by a human (who labels expected behaviour) and the
planning/review role (who labels the layer hypothesis and the "do not do"
list). They live under <<PROJECT: failure-briefs directory>> and feed the
project's failure-to-eval pipeline (the path from an observed failure to a
coverage case).
<!-- e.g. (csagent): the deliver agent labels the hypothesis; the pipeline
     is the case-family / bad-case pipeline. -->
<!-- e.g. (csagent): docs/diagnostics/failure-briefs/<brief-id>.md -->

Every brief has these six fields (keep all six — the structure is the
point):

- **What happened?** — the observed behaviour in one or two sentences, so
  a reader who never saw the trace understands the failure shape.
- **What should a good agent have done?** — the expected behaviour on the
  same input, from the user's perspective. Pins the failure to a contrast.
- **Why does this matter?** — the user / business / safety impact in one
  line; cite the constitution clause it violates if applicable. Forces a
  relevance check.
- **Is this a one-off or a pattern?** — `one-off` | `pattern` | `unknown`,
  with a one-line evidence note. Pattern failures justify a case family;
  one-offs usually do not justify a runtime change.
- **Which layer is likely responsible?** — one layer from §3, with a
  one-line justification. Forces the hypothesis to be explicit and
  disprovable instead of defaulting to a deterministic guard.
- **What should NOT be done?** — the tempting-but-wrong fix (typically a
  keyword / regex / if-else / enum / per-case matrix) and the clause it
  violates. Every brief encodes a guardrail against the short-term
  hardcode.

A worked example brief lives in <<PROJECT: worked-examples doc>> (Layer B
/ project examples), not here.

## 3. Fix Layer Classification Checklist

Use this checklist whenever a failure is observed and a fix is being
considered, **before any code is written**. Walk the questions in order;
**first match wins**. The point is not to find the "best" layer in the
abstract — it is to prevent every failure defaulting to a deterministic
guard.

### 3.1 Layer set

<<PROJECT: define the fix-layer set for YOUR architecture — the set,
names, count, and granularity are entirely project-defined. A useful set
routes each observed failure to exactly one owner and reserves
deterministic-guard changes for immutable-invariant violations.

As a thinking aid only (NOT requirements, NOT a fixed taxonomy), common
diagnostic dimensions a project MIGHT turn into layers: orchestration /
infra; the immutable deterministic-guard boundary; what the model was
shown (context); what state carried across turns; the model's own
decision; whether the eval / spec itself is valid; product / policy
decisions; eval- or judge-stability (if you use a model judge); and a
human-review escape hatch for "looks like a guard but no current invariant
covers it — do not invent one". Pick, drop, rename, split, or merge these
freely.>>
<!-- e.g. (csagent) chose a 9-layer set: infra | java_guard |
     prompt_projection | skill_state | semantic_planner | eval_spec |
     product_policy | judge_calibration | human_review_required. -->

### 3.2 Decision questions (first match wins)

<<PROJECT: an ordered list of yes/no questions, each routing to one §3.1
layer. The first-match-wins mechanic is portable; the ORDERING is
project-specific — sequence the questions so the failure modes your
architecture most needs to rule out first come first (e.g. safety /
policy, tool-contract, retrieval / data-freshness, context, or state,
depending on your system).>>
<!-- Heuristic (optional): ruling out structural / upstream causes before
     attributing a failure to the model's own judgment tends to prevent
     reflexive hardcoding — but the right order is yours to decide.
     e.g. (csagent), abbreviated:
     1. Session won't start / crash / timeout not caused by tool
        semantics? -> infra
     2. A current Tier-0 invariant is being broken? -> java_guard
        (if it LOOKS like guard territory but no current Tier-0 covers it
        -> human_review_required; do not invent a new Tier-0)
     3. Model chose validly but the context handed to it was wrong/
        impoverished? -> prompt_projection
     4. A multi-turn flow is losing state across turns? -> skill_state
     5. Model chose a semantically wrong action even with correct context
        and state? -> semantic_planner
     6. The eval CaseSpec / judge asks for something invalid/impossible?
        -> eval_spec
     7. The ask is a product/policy decision the runtime can't make? ->
        product_policy -->

**Tail rule (judge stability, PORTABLE mechanic):** if the same case flips
across reruns of the *same prompt and spec*, reclassify it as the
project's judge- / eval-stability layer regardless of which question
matched.

**Default tail (PORTABLE mechanic):** if no question matches cleanly →
the project's human-review escape hatch (§3.1).
<!-- e.g. (csagent): the judge-stability layer is judge_calibration; the
     escape hatch is human_review_required. -->

### 3.3 Why no deterministic guard by default (PORTABLE)

Most observed failures look like deterministic-guard territory because a
keyword / regex / if-else can paper over the symptom in one PR. The
iteration rule (§1.5) and the forbidden list (§1.7) rule this out for soft
semantic decisions: those belong to the model. A new deterministic guard
is only justified when it protects a current immutable (Tier-0) invariant.
If no current invariant covers it, the human-review escape hatch (§3.1) is
the correct exit — the human decides whether to open a new invariant or
push the fix back to a context / state / decision layer.

## 4. Anti-Hardcode Review

### 4.1 Anti-hardcode review kernel

The canonical copy-pastable review kernel lives at
`../B-process/anti-hardcode-review-kernel.template.md` (Layer B). It
contains the question set, a scope-exemption clause, and the verdict set.
The number of questions is project-defined there (it is not fixed by this
constitution).

### 4.2 Sprint-close review header (PORTABLE convention)

At sprint close, the review role writes a sprint-level decision to the top
of <<PROJECT: review-findings file>> using this header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```
<!-- e.g. (csagent) review-findings file: docs/codex-findings.md -->

The per-PR verdict (in the §4.1 kernel) and this sprint-close header are
different artefacts: the per-PR verdict reviews one PR; the header reviews
the sprint as a whole and gates closure.

### 4.3 Milestone-shared review cadence

→ Moved to Layer B: `../B-process/milestone-framework.md` §4.3. Cite as
"milestone-framework §4.3".

## 5. Eval Acceptance Rules

A sprint or PR is accepted only when its eval evidence clears every bar
below. This operationalizes §1.6: a visible pass-rate increase is not
enough.

### 5.1 Acceptance bars

Portable hard defaults (keep — true for any project):
- **In-scope / target cases pass** — the cases the change named as in
  scope.
- **No regression on related cases** — cases sharing the failure shape or
  surface must not regress.
- **Safety floor unchanged** — the immutable safety invariants stay green.

Eval partition model (project-specific):
- <<PROJECT: how you slice coverage beyond the three defaults above —
  e.g. dedicated negative-controls, and held-out cases not visible to the
  implementation role.>>
<!-- e.g. (csagent) partitions: target / neighbor / negative-control /
     shadow (held-out, dev-blind; readable only by human + review role). -->

Project-specific floors (add yours, if any):
- <<PROJECT: domain floors that must not regress — e.g. a grounding
  floor, wrong-containment rate, over-escalation rate, or
  architecture-health metrics>>.

### 5.2 Baseline pointer

The canonical baseline is <<PROJECT: baseline doc path>>. Verify the date
and run reference before trusting any specific number.
<!-- e.g. (csagent): docs/current_eval_baseline.md -->

### 5.3 Visible-eval vs held-out generalization (PORTABLE)

A visible-eval improvement that ships with a held-out / generalization
regression is a **fail**, not a pass. Enforces the §1.7 "do not optimize
visible eval at the cost of held-out generalization" line.
<!-- e.g. (csagent): the held-out partition is the dev-blind shadow set. -->

### 5.4 No eval-side override of a real bug (PORTABLE)

An eval-side override — widening the spec to accept the agent's actual
output, relaxing the rubric, or downgrading a judge — may NOT mask a
genuine agent mistake (§1.7). If the agent is wrong, fix the agent; if the
spec is wrong, fix the spec and document the override with its §3.1 layer
classification (the project's eval/spec layer).
<!-- e.g. (csagent): the eval/spec layer is eval_spec. -->

### 5.5 Hard gates vs observation (PORTABLE pattern)

**If — and only if —** a metric has accumulated confounds (e.g. provider
drift, judge variance, mock-vs-real gap) that prevent reliable
attribution, it MAY be demoted to **observation** (still computed and
tracked, but non-blocking). A project may demote nothing. The hard close
gates are:

- the §4.1 anti-hardcode kernel pass (per the §4 dispatch convention);
- the test suite shows no new regression beyond the documented baseline;
- the safety floor unchanged (§5.1);
- <<PROJECT: any additional hard floors — e.g. a grounding floor>>;
- the **primary acceptance gate** below (§5.6).

<<PROJECT: metric(s) demoted to observation, or "none">>.
<!-- e.g. (csagent): the 14-case smoke composite_score / pass-rate /
     judge dims are observation-only. -->

### 5.6 Primary acceptance gate

<<PROJECT: define the primary acceptance gate — the high-signal, typically
human-judgment gate that, together with the §5.5 hard gates, decides
close.>> Its mechanics (schema, tiering, lifecycle, review process) live
in Layer B → `../B-process/badcase-lifecycle.md`. Cite as
"badcase-lifecycle §5.6".
<!-- e.g. (csagent): a curated bad-case suite with manual trace review.
     Other projects might use a red-team suite, live canaries, a benchmark
     gate, or human QA. -->

### 5.7 Eval evidence gate (PORTABLE)

Mocked-model tests cannot be primary evidence that a prompt change caused
a behaviour change — the mock controls the measured variable. A real-model
rerun is the eval evidence gate; mocked tests cover projection / rendering
/ dispatch wiring only.

## 6. Architecture-health metrics

**If your project defines** architecture-health metrics (a §5.1 project
floor), their definitions live in Layer B →
`../B-process/architecture-health-metrics.template.md` §6 (cite as
"architecture-health-metrics §6"), and the corresponding §5.1 floor
consults them once collection lands. Projects without such metrics may
omit this.

## 7. Required sprint-objective stanza

A **semantic-touching sprint** changes the prompt, a runtime semantic
decision, the eval spec, or judge calibration. Pure infra / docs-only /
config-governance / characterization-test sprints are **exempt**.
Semantic-touching sprints MUST include the stanza below in the active
sub-sprint contract; review checks for it as part of scope.

### 7.1 Stanza template (PORTABLE structure)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** <one of the §3.1 layers>

**Tier-0 invariant:** <"This sprint adds no Tier-0 invariant." OR a
pointer to the invariant being protected in the project's risk-policy doc>

**Semantic hardcode:** <"No semantic hardcode introduced." OR "Introduces
<named hardcode>; justification: <reason>; sunset plan: <downgrade-to-
signal trigger + target sprint id>">

**Generalization coverage:** <coverage stated in terms of your §5.1 eval
partition model, OR "case family not yet built; deferred to <sprint id>">
```
<!-- e.g. (csagent) coverage line: "target / neighbor / negative / shadow
     case counts: <T>/<N>/<G>/<S>". -->

Each field has one acceptable form. A sprint that cannot fill a field
without a stretch has not yet decided what it is doing; re-scope before
the implementation role runs.

**Multi-layer prospective variant (PORTABLE):** investigation +
bundle-or-defer sprints span multiple candidate layers; the stanza is then
per-decision-outcome multi-layer prospective (enumerate the possible §3.1
layers per case). Bundle policy must live in the sub-sprint contract so
review can verify scope discipline.

### 7.2 Worked example

→ A filled worked example lives in <<PROJECT: worked-examples doc>>
(Layer B / project examples), not here.

## 8. Milestone framework

→ Moved to Layer B: `../B-process/milestone-framework.md` §8. Cite as
"milestone-framework §8".

## 9. Agent prompt artifact rules

→ Moved to Layer B: `../B-process/prompt-artifact-rules.md` §9. Cite as
"prompt-artifact-rules §9".
