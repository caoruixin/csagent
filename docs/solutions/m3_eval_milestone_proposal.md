---
title: M3-Eval — Coarse-to-Fine Evaluation Architecture
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (until milestone_objective.md is drafted by the deliver agent)
last_reviewed: 2026-05-20
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Research-agent proposal for the M3-Eval milestone candidate. Produced 2026-05-20 by
  a research agent after Path-1 investigation (eval harness deep-dive + per-UC critical
  flow review + case-spec design philosophy review + milestone/R-item alignment audit).
  Human decisions locked 2026-05-20 (see §4). The deliver agent picks up from here to
  draft `docs/milestone_objective.md` (M3-Eval) and the first sub-sprint's
  `docs/sprint_objective.md` (S-Eval-1). The corpus audit work referenced in §12 is
  intentionally separated into an independent M3-Corpus milestone candidate.
---

# M3-Eval — Coarse-to-Fine Evaluation Architecture

> **Reading order for the deliver agent.** §1 explains why this proposal exists.
> §2 names the target architecture. §3 maps current state to target. §4 lists the
> human's locked decisions (these are non-negotiable inputs to milestone drafting).
> §5 is a deep-dive on decision 5 (LLM-visible critical_steps) because it is the
> most architecturally consequential decision and shapes S-Eval-2 / S-Eval-3 scope.
> §6 is the sub-sprint breakdown the deliver agent will turn into sprint contracts.
> §7-§14 are operational details (Codex cadence, acceptance bar, risks, R-item map,
> companion milestone, deliverables, references).

## 1. Why this proposal exists

### 1.1 Observed: current smoke results are not reliably attributable to the bot

The 14-case smoke run at `eval_interactive/results/20260518-091559/` shows 14/14
FAIL with `mean_composite_score = 0.0`. Inspection of `case_results[].llm_calls`
reveals every LLM call has `success: false` and `errorMessage: "[llm_transport_error]
LLM API call failed after retry (provider=kimi)"`. Every bot turn output is
"Sorry, I'm having trouble reaching the assistant right now. Please try again
in a moment." This is an external LLM-provider outage, not a bot-quality
regression. It is the exact pattern that
[`docs/current/iteration_governance.md`](../current/iteration_governance.md)
§5.5 (2026-05-16) cited when demoting the smoke `composite_score` from a hard
gate to an observation — provider drift cannot be attributed to sprint-side
code changes.

### 1.2 Observed: CaseSpec schema encodes hard gates that the LLM-first constitution does not want

[`eval_interactive/eval_interactive/case_spec/schema.py`](../../eval_interactive/eval_interactive/case_spec/schema.py)
`Expected` dataclass still treats six surfaces as hard or mandatory inputs,
despite §5.5/§5.6 having moved the primary gate elsewhere:

| Field | Current behaviour | Why this is the user's concern |
|---|---|---|
| `expected.expected_tool_sequence: list[str]` | Drives L2 `tool_sequence_match` LCS score | Encodes a *means* (specific tool order) as if it were the *end* (user goal solved) |
| `expected.forbidden_tools: list[str]` | L1 hard-gate via `no_forbidden_tools` | Re-encodes runtime tool-policy per case; conflates safety floor with case-specific narrative |
| `expected.escalation_trigger: EscalationTrigger` | Exact-match against 23-value enum (family-relaxed) | Multiple valid triggers may apply to the same case; spec picks one |
| `expected.bot_handling_pattern: str` | Plain-English narrative that the L3 judges consume | Encodes specific tool calls and phrasing as expectations |
| `scoring.outcome_checks: list[str]` | 7-item identical checklist per case | No room for "this case is about outcome, not process" |
| `expected.should_escalate: bool` | Binary | "Resolve OR escalate equally valid" cases must be approximated via `acceptable_outcomes` |

### 1.3 Observed: per-UC critical-flow correctness is not evaluated today

For the user's "post not found" scenario across UC-A (visibility) / UC-FP
(deleted, explain) / UC-H (deleted, appeal intake), the eval today has **three
indirect signals only**:

- `correct_uc` — answers "did routing land in the right UC?" but not "did the
  bot check ad_id before deciding?"
- `tool_sequence_match` — proxies process correctness via exact tool order;
  conflates "right pattern" with "right specific sequence"
- L3 `groundedness` — looks at the final reply, not the decision path

There is **no check today** that asks "did the bot consult
`get_moderation_review_context` before explaining a deletion?" or "did the bot
collect `ad_id` + `email` before `request_handover` on UC-H?". The current
design relies on the LLM doing this implicitly from tool whitelists + retrieval
results, with `tool_sequence_match` as a brittle proxy.

### 1.4 Observed: M2 just landed the Skill abstraction; eval has not consumed it

M2 closed 2026-05-18 (clean A PASS, milestone-shared Codex `pass / 0 blocking`).
Six Skill YAMLs now live under `server/src/main/resources/skills/` with
LLM-visible `procedure` fields and machine-readable `guardrails`. The eval
harness reads neither field. This is **already-shipped declarative semantic
content** that the new eval architecture can stand on without inventing new
abstractions.

### 1.5 Observed: governance is one step ahead of schema; schema needs to catch up

`iteration_governance.md` §5.5/§5.6 have already declared:

- smoke `composite_score` is observation-only (§5.5)
- curated bad-case suite is the new primary acceptance gate, human-judgment
  (§5.6, §5.6.1, §5.6.2)
- tier model for bad cases: core / scope-relevant / closed-as-regression-guard
  / archived (§5.6.1, §5.6.3)

The CaseSpec schema, the L1/L2/L3 scoring code, and the casespec generation
pipeline (`eval_interactive/scripts/regenerate_case_specs.py` + L1/L2/L3
three-layer override pipeline) **have not been re-aligned to this governance**.
This milestone closes that gap.

## 2. Proposed three-tier architecture

The current L1 / L2 / L3 layered model treats checks as roughly equal-weighted
peers (with conditional mandatory flags). This proposal replaces that with a
**four-tier pyramid** where tier ordering encodes "coarse-to-fine, mandatory-to-
observational":

### 2.1 Tier-0 — Safety floor (unchanged, hard gate)

Existing checks preserved verbatim:

- `no_pii_leakage`
- `no_human_only_tool_exposure`
- `no_critical_policy_violation` (forbidden-phrase scanner: "I've fixed",
  "I'll restore", "I've banned the user", etc.)
- `escalation_compliance` (mandatory escalation on high/critical risk; canonical
  `escalation_reason` family matching)
- `phase_transition_validity`

These are Tier-0 invariants per
[`docs/runtime_freeze_and_risk_policy.md`](../runtime_freeze_and_risk_policy.md)
§1/§2. Eval verifies their presence; runtime owns enforcement. **Not touched
by this milestone.**

### 2.2 Tier-1 — Outcome (new mandatory signal, coarsest, most reliable)

One question: **"Did the bot solve the user's problem, appropriately escalate,
or appropriately defer?"**

Evaluated by:

- **Bad-case suite manual review** (already §5.6 — milestone-close human-
  judgment gate against `closure_criterion`)
- **`anchor_outcome` suite** (NEW, see S-Eval-1) — outcome-only specs covering
  each UC's representative path; no tool-sequence / efficiency / preservation
  checks
- **L3 `user_goal_achievement`** judge dim (NEW, see S-Eval-5) — supplementary
  advisory 1-5 signal

CaseSpec minimum required surface for Tier-1: `expected.outcome_class`,
`persona.user_goal_summary`, `closure_criterion`.

### 2.3 Tier-2 — Critical-flow (NEW, UC-specific, selective)

Per-UC process correctness. **Not encoded in CaseSpec.** Derived from each
Skill's `critical_steps` array (NEW field on Skill YAML, see S-Eval-2). Each
step declares:

```yaml
critical_steps:
  - id: consult_moderation_context_before_explaining
    desc: |
      For UC-FP issues, consult the moderation context before
      explaining the removal reason. The reason narrative depends
      on the moderation `reason_code`.
    trace_check: any_of(
      session.moderation_context_present,
      accumulated_tool_results.get_moderation_review_context
    )
    mandatory_for: [UC-FP]
    severity: mandatory   # mandatory | advisory
```

A new eval-side `SkillProcedureExtractor` reads a session trace plus the active
Skill, then evaluates each `trace_check` against the trace, producing per-step
PASS / FAIL / N/A. A Tier-2 fail occurs when a `mandatory` step fails for a
case whose active Skill matches the step's `mandatory_for` UC set.

**Decision-locked: `critical_steps[].desc` is LLM-visible** (see §5 below). The
trace_check DSL is eval-side only.

### 2.4 Tier-3 — Polish (advisory only, observation)

Demoted from the old L2/L3:

- Tier-3 process polish: `turn_efficiency`, `tool_sequence_match` (kept but
  diagnostic only), `issue_preservation`, `handover_completeness` field
  completeness
- Tier-3 language polish: L3 `relevance`, `tone_appropriateness`, `groundedness`
  (all kept for trend tracking, none gate `case_passed`)
- Tier-3 system: latency, token cost, stall rate

**Never** flips `case_passed`. Surfaces in dashboards, trend tables, and
sprint-close conversations only.

## 3. Gap check — current implementation vs proposed architecture

| Surface | Current | Proposed | Gap type |
|---|---|---|---|
| Smoke composite_score gate | §5.5 demoted in text; schema + harness still gate-style | Explicit `anchor_outcome` suite (Tier-1 only); smoke renamed/repositioned as observation | Text → schema/code |
| Bad-case suite | 1 case (Alice UC-A/UC-H) + manifest | 10-12 cases across D1-D4 + 3 UC families; trial milestone-close dry-run done | Quantity + process |
| Per-UC critical flow | Not evaluated; `tool_sequence_match` proxy only | Tier-2 via Skill `critical_steps`, auto-derived | **New capability** |
| CaseSpec over-constrained fields | 6 hard surfaces (see §1.2) | All optional / Tier-3 advisory; new `closure_criterion` optional field | Schema convergence |
| L3 judge stability | `relevance` / `tone_appropriateness` deferred but still write composite | All three L3 dims advisory-only; new `user_goal_achievement` | Weight rearrangement |
| Skill `procedure` field | M2-landed, LLM-visible, eval not consuming | Eval reads via `SkillProcedureExtractor` of `critical_steps`; LLM also reads `critical_steps[].desc` | **Existing resource not consumed** |
| FAQ corpus coverage | R-corpus-coverage-audit-per-uc open | Separated to M3-Corpus (see §12) | Out of scope here |
| `expected_tool_sequence` | L2 LCS scored | Optional; diagnostic only when present | Field demotion |
| `forbidden_tools` | L1 hard gate | Optional; safety floor stays in runtime tool-policy | Field demotion |

## 4. Locked decisions (2026-05-20)

The human has locked the following five decisions. These are inputs the
deliver agent should treat as non-negotiable.

| # | Decision | Implication |
|---|---|---|
| 1 | **Walk-A**: independent M3-Eval milestone with 5 sub-sprints | Whole work tracked as one §8 milestone; Codex defaults to milestone-shared review |
| 2 | **M3-Corpus** is a separate milestone (not bundled here) | Corpus / data work decoupled from schema / harness work |
| 3 | **Bad-case suite scale**: 10-12 cases this milestone | S-Eval-4 does not need a pre-harvesting phase; sources from the 29 approved entries in `case_spec_overrides.yaml` |
| 4 | **Corpus audit independent** | Can run in parallel under a different dev / review path; no dependency between M3-Eval and M3-Corpus close |
| 5 | **`critical_steps` is LLM-visible** — `desc` appears in the prompt alongside Skill `procedure` | S-Eval-2 must include projection wiring; S-Eval-3 anti-hardcode review becomes the milestone's critical gate (see §5) |

## 5. Decision-5 deep-dive — LLM-visible critical_steps

Decision 5 has the largest architectural consequence and shapes both
S-Eval-2 scope (projection wiring) and S-Eval-3 scope (anti-hardcode review
posture). The deliver agent should brief dev and review agents on the
following four points explicitly.

### 5.1 Dual role of `critical_steps`

- **LLM-side**: each step's `desc` (human-readable narrative) is rendered into
  `phase_plan.skill` in the per-turn projection (alongside `procedure`). The
  LLM uses it as procedural guidance — "for UC-FP, consult moderation context
  before explaining" — exactly the way `procedure` is used today.
- **Eval-side**: `SkillProcedureExtractor` reads `trace_check` (DSL) against
  the trace and emits Tier-2 PASS / FAIL / N/A.

Same YAML file, two consumers. **Both can change only by editing the Skill
YAML**, which eliminates the doc-drift mode where procedure narrative and
eval expectation diverge.

### 5.2 Single source of truth

This is the design property that makes the proposal worth more than its parts.
Today the same procedural intent is encoded in:

- `system_prompt.txt` teaching paragraphs (Sprint 31 / Sprint 33; partly
  migrated to `discover_triage.yaml` in S40)
- `PhaseEvaluator` per-UC `allowedTools` filter (runtime; Java)
- `IntakeFieldsRegistry` per-UC field set (runtime; Java)
- `ToolPolicyEnforcer` per-UC tool gating (`tool-policy.yaml`)
- CaseSpec `expected_tool_sequence` (per-case; eval)
- L3 judge `bot_handling_pattern` narrative (per-case; eval)
- Fixed Script Library v1 (templates; runtime)

By centralising "what the bot should do, step by step" into Skill YAML, the
new architecture **reduces the number of writeable surfaces from six to one**
for declarative process content. Existing surfaces remain as enforcement
mechanisms (runtime / safety floor), but the **declarative intent** lives in
Skill YAML.

### 5.3 §1.7 anti-hardcode red line

Because `critical_steps[].desc` enters the prompt, it crosses
[`docs/current/iteration_governance.md`](../current/iteration_governance.md)
§1.7 forbidden-list territory. The deliver agent should brief dev and review
agents on this judgment standard explicitly:

| Form of `desc` | Example | Verdict |
|---|---|---|
| Soft procedural narrative | "For UC-FP issues, consult the moderation context before explaining the reason" | ✅ LLM-first; procedural guidance the LLM owns acting on |
| Soft diagnostic signal | "If the user mentions 'appeal' or 'review', consider whether this is actually UC-H rather than UC-FP" | ⚠️ Edge — Codex will scrutinise; acceptable as guidance, not as a rule |
| Hard if-else rule | "IF user.message.contains('appeal') THEN active_use_case := UC-H" | ❌ Violates §1.7; Codex rejects |
| Keyword enumeration matrix | "Trigger words for UC-H: ['appeal', 'review', 'wrongly', 'unfairly', ...]" | ❌ Violates §1.7; Codex rejects |

Every `critical_steps[].desc` written in S-Eval-3 must pass this standard.
Codex per-sub-sprint review is triggered for S-Eval-3 specifically because of
this risk (see §7).

### 5.4 Token cost

LLM-visible `critical_steps` increase projection size:

- 6 Skills × 3-5 critical_steps each × ~30 tokens per `desc`
- ≈ **500-900 tokens of additional projection per turn**

The current `system_prompt.txt` is 80 lines (post-S40 extraction). This
increment is comparable in magnitude to past Sprint 31 / Sprint 33 teaching
additions and is acceptable. The deliver agent should record the actual
post-milestone token increment as an M3-Eval observation metric.

## 6. M3-Eval sub-sprint scope

Five sub-sprints in dependency order. The deliver agent will expand each into
a `docs/sprint_objective.md` (current) + dev/review prompts when picking the
sub-sprint up; this section names the locked scope.

### S-Eval-1 — Schema simplification + outcome-only anchor suite

**Layer (per §3 classification)**: `eval_spec`
**Estimated dev**: 2-3 days + Codex review

**Scope**:

1. In `eval_interactive/eval_interactive/case_spec/schema.py`, change four
   `Expected` fields from required/mandatory-effect to optional with
   default-off behaviour: `expected_tool_sequence`, `forbidden_tools`,
   `bot_handling_pattern`, `escalation_trigger` (relax exact-match into
   "if present, used for diagnostic; if absent, skip"). Existing 14 smoke
   cases + 30 anchor cases must continue to load unchanged.
2. In `eval_interactive/eval_interactive/scoring/`, demote L1
   `no_forbidden_tools` to a Tier-3 advisory when `forbidden_tools` is
   present, and skip it entirely when absent. Demote L2 `tool_sequence_match`
   to Tier-3 diagnostic. Demote L1 `escalation_reason_consistency` family-
   matching to advisory when `escalation_trigger` is absent.
3. Create `eval_interactive/case_specs/anchor_outcome/` with 10-15 cases,
   one to three per UC, each declaring only `outcome_class`, persona
   (including `user_goal_summary`), and a new optional `closure_criterion`
   string. **No** `expected_tool_sequence`, **no** `forbidden_tools`, **no**
   `bot_handling_pattern`. Reuse `source_session_id`s from existing smoke /
   anchor where representative.
4. Update [`docs/current/iteration_governance.md`](../current/iteration_governance.md)
   §5.5 to state "anchor_outcome is the second human-judgment surface beside
   bad-case suite; smoke remains observation-only by design".

**§7 stanza pre-fill**:

```
Target failure layer: eval_spec
Tier-0 invariant: This sub-sprint adds no Tier-0 invariant.
Semantic hardcode: No semantic hardcode introduced. All changes are
relaxations: fields move from required to optional, hard gates move to
advisory. New anchor_outcome suite contains only outcome declarations.
Generalization coverage: target = 4 schema fields demoted + 10-15 new
anchor_outcome cases; neighbor = existing 14 smoke + 30 anchor backwards-
compat verification; negative = one case with deliberately-wrong
expected_tool_sequence must NOT fail anymore; shadow = downstream
sub-sprints exercise the loosened schema cumulatively.
```

**Out of scope for S-Eval-1**: deleting old fields entirely (kept for
backward-compat); rewriting any L3 judge dim (deferred to S-Eval-5).

---

### S-Eval-2 — Skill `critical_steps` schema + extractor + projection wiring

**Layer**: `eval_spec` (extractor) + `prompt_projection` (LLM-visible
wiring) — multi-layer, name `eval_spec` as primary
**Estimated dev**: 4-5 days + Codex review

**Scope**:

1. Extend the Skill YAML schema to include optional
   `critical_steps: list[CriticalStep]` per Skill (6 YAMLs in
   `server/src/main/resources/skills/`). `CriticalStep` carries `id`,
   `desc` (LLM-visible), `trace_check` (DSL string), `mandatory_for`
   (UC list), `severity` (mandatory | advisory). **This sub-sprint does
   not fill any `critical_steps` content** — only the contract.
2. Extend `SkillLoader` / `SkillRegistry` to validate and expose the new
   field. No behavioural change in dispatch.
3. Extend `ContextProjectionBuilder` to render `critical_steps[].desc`
   inside `phase_plan.skill` in the per-turn projection, immediately
   after `procedure`. Empty array → no projection change.
4. Create `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
   with a `SkillProcedureExtractor` class. It accepts a trace + active
   Skill and returns per-step PASS / FAIL / N/A. Define a minimal
   `trace_check` DSL: `accumulated_tool_results.<tool>`,
   `tool_event_seq(<tool_a>) < tool_event_seq(<tool_b>)`,
   `intake_state.fields_collected.contains(<field>)`,
   `session.<flag>_present`, `any_of(...)`, `all_of(...)`. Reject any
   regex / keyword-list / message-content matching in DSL — that would
   constitute a hardcode (§1.7).
5. Add Tier-2 `skill_procedure_followship` check that integrates the
   extractor result into composite scoring (mandatory failure → Tier-2
   fail). Empty `critical_steps` → always PASS.

**§7 stanza pre-fill**:

```
Target failure layer: eval_spec (primary) + prompt_projection
Tier-0 invariant: This sub-sprint adds no Tier-0 invariant.
Semantic hardcode: No semantic hardcode introduced. Contract +
projection wiring + extractor only; no content lands until S-Eval-3.
The trace_check DSL is structurally constrained to NOT permit regex /
keyword / message-content matching, enforced by the DSL parser.
Generalization coverage: target = schema contract + extractor + projection
wiring; neighbor = existing 14 smoke + 30 anchor pass unchanged (empty
critical_steps); negative = a critical_step with deliberately-invalid DSL
must fail schema validation; shadow = S-Eval-3 exercises with content.
```

**Out of scope for S-Eval-2**: filling any `critical_steps` content (deferred
to S-Eval-3); modifying any existing Skill `procedure` text; new SkillRegistry
behavior beyond field exposure.

---

### S-Eval-3 — Populate `critical_steps` for the 6 Skills

**Layer**: `eval_spec` (content under an LLM-visible contract that S-Eval-2 wired)
**Estimated dev**: 5-6 days + per-sub-sprint Codex review (§4.3 trigger #2)

**Scope**:

1. For each of the 6 Skills (`discover_triage`, `resolve_faq_grounded_answer`,
   `resolve_intake_collect_and_handover`, `confirm`, `escalate`, `terminal`),
   populate 3-5 `critical_steps`. Focus coverage on:
   - UC-FP "post deleted" decision tree (consult moderation context before
     explaining; provide reposting guidance when actionable; route to UC-H
     intake on appeal request)
   - UC-H / UC-J / UC-K "intake then case then handover" sequence
     (`create_case_controlled` before `request_handover`; intake fields
     collected before case creation)
   - UC-A / UC-B / UC-C / UC-D / UC-E / UC-F FAQ-grounded answer (retrieve
     before answering; cite source on factual_answer output class)
   - DISCOVER disambiguation (clarification-budget respect; no committal
     to UC before sufficient evidence)
2. Each `desc` written as soft procedural narrative per §5.3 standard. The
   deliver agent must brief dev to draft `desc` as "what a good CS agent does
   in this situation", never as "if user says X then do Y".
3. Run the existing bad-case suite (Alice) + the new `anchor_outcome` suite
   (from S-Eval-1) against the populated `critical_steps`. Verify the Tier-2
   failures align with known failure modes documented in
   `eval_interactive/case_specs/bad_cases/_manifest.md` (Alice failure shape
   should surface as `intake_complete_required` Tier-2 fail on UC-H).

**§7 stanza pre-fill**:

```
Target failure layer: eval_spec
Tier-0 invariant: This sub-sprint adds no Tier-0 invariant.
Semantic hardcode: Introduces 18-30 critical_steps `desc` fields as
LLM-visible soft procedural narratives. Justification: replaces implicit
reliance on tool whitelists + retrieval results with explicit procedural
guidance the LLM owns acting on. NO if-else, NO keyword matrix, NO regex.
Anti-hardcode standard per the proposal §5.3 applies; Codex per-sub-sprint
review verifies. Sunset plan: N/A — this IS the soft-signal form that
replaces existing scattered teaching paragraphs in system_prompt.txt and
the implicit reliance on tool whitelisting.
Generalization coverage: target = 18-30 critical_steps across 6 Skills;
neighbor = Alice bad case + anchor_outcome suite Tier-2 alignment;
negative = a UC-A case must not trigger UC-FP critical_steps (active
Skill scoping); shadow = 3-5 cases held out of dev visibility for
generalization check at M3-Eval close.
```

**Out of scope for S-Eval-3**: modifying `procedure` field text of any
Skill; new Skill files; modifying `SkillGuardrailDispatcher`; adding
runtime if-else.

---

### S-Eval-4 — Bad-case suite expansion + trial milestone-close dry-run

**Layer**: `eval_spec` (data; not code)
**Estimated dev**: 5-7 days (heavy in human-review collaboration time)

**Scope**:

1. Select 10-12 entries from `eval_interactive/case_spec_overrides.yaml` whose
   `status: approved` block has the strongest semantic divergence from the
   pre-override CaseSpec. Prefer entries that span the four bad-case
   dimensions (D1 mis-classification / D2 intake prefill gap / D3 lock-in
   escape / D4 stated_reason circularity) and the three highest-volume UC
   families (UC-FP, UC-G, UC-H).
2. For each selected entry, author a bad-case YAML in
   `eval_interactive/case_specs/bad_cases/` with full
   `bad_case_metadata` (`source_session_id`, `surfaced_by`, `surfaced_date`,
   `failure_shape`, `layers_involved`, `closure_criterion`,
   `tier: core | scope-relevant`).
3. Update `eval_interactive/case_specs/bad_cases/_manifest.md` lifecycle
   ledger with the 10-12 new rows.
4. Run a **trial milestone-close manual review dry-run**: deliver agent +
   human walk through each new bad case's trace and judge PASS / FAIL /
   IMPROVING against `closure_criterion`. The purpose is to calibrate
   `closure_criterion` wording, not to verify bot correctness. Findings
   feed back as `_manifest.md` notes.

**§7 stanza pre-fill**:

```
Target failure layer: eval_spec
Tier-0 invariant: This sub-sprint adds no Tier-0 invariant.
Semantic hardcode: No semantic hardcode introduced. Bad cases are
evaluation samples / data, not code.
Generalization coverage: target = 10-12 new bad cases; coverage spans
D1-D4 dimensions and UC-FP / UC-G / UC-H families; neighbor = existing
Alice case (re-run, must not regress); negative = N/A at this layer;
shadow = none required.
```

**Out of scope for S-Eval-4**: synthesising bad cases from non-real
sessions; touching the existing Alice case; modifying any Tier-0 / Tier-1 /
Tier-2 check code (those are S-Eval-1 / S-Eval-2 / S-Eval-3 territory).

---

### S-Eval-5 — L3 judge repositioning + R-item closure

**Layer**: `eval_spec` (judge config + rubric)
**Estimated dev**: 2-3 days + Codex review

**Scope**:

1. In `eval_interactive/eval_interactive/scoring/llm_judge.py`, demote the
   three current L3 dimensions (`relevance`, `tone_appropriateness`,
   `groundedness`) from composite contributors to Tier-3 advisory. Their
   numeric scores are still recorded; they no longer factor into `case_passed`
   or `composite_score`.
2. Add a new L3 dimension `user_goal_achievement` (coarse 1-5: did the bot
   help the user achieve the stated `persona.user_goal_summary`, or
   appropriately escalate / defer?). Wire as Tier-1 supplementary
   advisory signal.
3. Update the rubric prompts:
   - Close R-l3-judge-form-context-trust-rubric: explicitly state
     `form_context.first_name` is a trusted signal; the bot is allowed to
     greet by first name without confirmation.
   - Close R-l1-source-citation-quality-rubric: tighten the citation
     check to require `canonical_url` OR article title; reject bare
     Salesforce IDs as sole citation evidence.
4. Close R-cs040-l3-review-intake-completion-semantics and
   R-cs038-l3-review-intake-efficiency by routing them through the new
   tiered architecture (their schema-block dependency is resolved by the
   S-Eval-1 schema simplification).

**§7 stanza pre-fill**:

```
Target failure layer: eval_spec
Tier-0 invariant: This sub-sprint adds no Tier-0 invariant.
Semantic hardcode: No semantic hardcode introduced. Rubric updates are
narrative refinements; tier repositioning is a weight rearrangement.
Generalization coverage: target = 3 L3 dims demoted + 1 new dim +
rubric updates; neighbor = bad-case suite + anchor_outcome suite must
not regress; negative = a previously-PASS case must not flip to FAIL
on the new rubric (repositioning is a relaxation); shadow = run on
held-out anchor cases.
```

**Out of scope for S-Eval-5**: changing L3 model temperature or provider
(deferred per Sprint 4); retraining or replacing the judge model;
modifying L3 prompt structure beyond rubric wording for the four R-items.

## 7. Codex review cadence

Per [`docs/current/iteration_governance.md`](../current/iteration_governance.md)
§4.3:

- **Default**: milestone-shared review at M3-Eval close. Single cumulative
  Codex pass over commit range covering S-Eval-1 through S-Eval-5.
- **Trigger fires for S-Eval-3 → per-sub-sprint Codex review required**:
  §4.3 trigger #2 (§1.7 forbidden-list adjacent). Codex must independently
  verify each `critical_steps[].desc` against the §5.3 standard at S-Eval-3
  close, **before** S-Eval-4 begins. This is the milestone's critical
  governance gate.
- S-Eval-1 / S-Eval-2 / S-Eval-4 / S-Eval-5 defer to milestone close unless
  the deliver agent + human surface a per-sub-sprint trigger ad hoc.

## 8. M3-Eval close acceptance bar

The deliver agent encodes the following as `milestone_objective.md` §5.
Close decision is PASS only if all items below clear:

- [ ] Tier-0 safety floor verified: `no_pii_leakage`,
      `no_human_only_tool_exposure`, `no_critical_policy_violation`,
      `escalation_compliance`, `phase_transition_validity` all PASS on the
      14-case smoke + 30-case anchor + new `anchor_outcome` + 10-12 new
      bad cases. Zero regressions.
- [ ] Codex anti-hardcode review PASS at milestone close (milestone-shared)
      AND at S-Eval-3 close (per-sub-sprint).
- [ ] 10-12 new bad cases landed in `eval_interactive/case_specs/bad_cases/`
      with full `bad_case_metadata`. Trial milestone-close manual review
      dry-run completed and recorded in `_manifest.md`.
- [ ] All 6 Skills carry populated `critical_steps`. Projection wiring
      working (LLM sees `critical_steps[].desc` in `phase_plan.skill`).
      `SkillProcedureExtractor` runs without crash across the full
      bad-case + anchor + smoke surface.
- [ ] R-items closed: R-l3-judge-form-context-trust-rubric,
      R-l1-source-citation-quality-rubric,
      R-cs040-l3-review-intake-completion-semantics,
      R-cs038-l3-review-intake-efficiency.
- [ ] Schema simplification backward-compatible: existing 14 smoke cases
      + 30 anchor cases load and run without modification.
- [ ] L3 repositioning is monotone-relaxing: no previously-PASS case flips
      to FAIL under the new tiering. (Verified by running smoke + anchor
      with both old and new scoring code in S-Eval-5.)
- [ ] Token-cost observation recorded: per-turn projection size before
      M3-Eval vs after (with populated `critical_steps`). Expected
      increment ≤ 1000 tokens; investigate if higher.

## 9. Out of scope

The deliver agent encodes the following in `milestone_objective.md` §4
non-goals:

- **No new Tier-0 invariants.** §1.6 / §1.7 boundary; nothing in this
  milestone elevates a runtime check to Tier-0.
- **No L3 judge model / temperature / prompt-structure retraining.**
  Deferred per Sprint 4; only the rubric narrative and tier weight change.
- **No CaseSpec field deletions.** Demoted fields stay optional for
  backward compat; deletion is a future fold-back, not this milestone.
- **No runtime if-else / regex / keyword matrix.** §1.7 red line.
- **No SkillRegistry / SkillLoader / SkillStateBus semantic change.**
  M2 just landed these; M3-Eval only augments the YAML schema.
- **No synthesised FAQ articles.** Corpus work is M3-Corpus territory.
- **No corpus audit work.** M3-Corpus territory.
- **No Salesforce handover orchestrator work.** M3-B territory; release
  gate not touched by this milestone.
- **No new escalation_reason enum values.** Runtime contract not touched.

## 10. Risks + compounding effects

### 10.1 Risk table

| Risk | Severity | Mitigation |
|---|---|---|
| Schema simplification breaks the 14 smoke / 30 anchor cases on load | High | S-Eval-1 uses optional fields; backward-compat is a S-Eval-1 acceptance condition |
| `trace_check` DSL expressivity is too thin | Medium | S-Eval-2 ships minimal DSL; if a needed expression is missing in S-Eval-3, the deliver agent extends DSL in S-Eval-3 within Codex review boundary; reject regex / keyword-content matching |
| Bad-case expansion lands too fast and `closure_criterion` quality is poor | Medium | S-Eval-4 dry-run trial close calibrates wording; cap at 10-12 cases, not 30+ |
| L3 repositioning silently changes historical baselines | Low | Repositioning is monotone-relaxing; per-dim score still recorded; an explicit "old composite" column kept for cross-run comparison through M3-Eval close |
| `critical_steps[].desc` is written as a hardcode and Codex misses it | Critical (§1.7) | Codex per-sub-sprint review on S-Eval-3 is the gate; dev must self-check against §5.3 standard before each commit |
| LLM-visible `critical_steps` blow up token cost beyond +1000 tokens | Low | Token-cost observation in §8 acceptance bar; if exceeded, deliver agent + human decide whether to trim or accept |
| M2 Skill abstraction introduces a hidden deviation that S-Eval-2 surfaces | Low | M2 closed clean (`pass / 0 blocking`); S-Eval-2 augments YAML, no SkillRegistry / SkillLoader semantic change |

### 10.2 Compounding effects (dependency order)

```
M2 (already landed)
  └─→ S-Eval-1 (schema simplification)
        └─→ S-Eval-2 (critical_steps schema + extractor + projection wiring)
              └─→ S-Eval-3 (critical_steps content)
                    └─→ S-Eval-4 (bad-case expansion; uses Tier-2 signals
                    |             to verify closure_criterion accuracy)
                    └─→ S-Eval-5 (L3 repositioning; benefits from
                                   anchor_outcome suite being live)
```

S-Eval-1 must finish before S-Eval-2 (schema dependencies). S-Eval-2 must
finish before S-Eval-3 (content needs contract + projection live). S-Eval-4
and S-Eval-5 can run in either order after S-Eval-3, but the deliver agent
should ship S-Eval-4 before S-Eval-5 so the bad-case suite + trial close
dry-run informs the L3 rubric tightening.

## 11. R-item mapping

### 11.1 R-items closed by M3-Eval

- `R-l3-judge-form-context-trust-rubric` — closed in S-Eval-5
- `R-l1-source-citation-quality-rubric` — closed in S-Eval-5
- `R-cs040-l3-review-intake-completion-semantics` — unblocked by S-Eval-1
  schema simplification, closed in S-Eval-5
- `R-cs038-l3-review-intake-efficiency` — unblocked by S-Eval-1 schema
  simplification, closed in S-Eval-5

### 11.2 R-items unblocked but not closed (graduated to follow-on)

- `R-case-spec-overrides-schema-scoring-extension` — schema-block lifted by
  S-Eval-1; full closure depends on either (a) a follow-on sprint that adds
  scoring metadata to override entries, or (b) M3-Corpus deciding overrides
  no longer need scoring extension. Track as carry-over.
- `R-escalation-reason-runtime-evidence-contract-review` — touched by
  S-Eval-1 demotion of `escalation_trigger` exact-match; full closure still
  requires runtime evidence on family-matching (out of scope).

### 11.3 R-items intentionally not touched

- `R-corpus-coverage-audit-per-uc` — M3-Corpus territory (§12)
- `R-faqMissCount-threshold-and-timing-review` — runtime config governance;
  not eval-spec
- `R-smoke-regression-investigation` follow-ons (`R-slow-llm-placeholder-coalesce`,
  `R-uc-k-intake-complete-case-id-binding`,
  `R-prompt-phase-plan-directive-followship`) — runtime layers; not
  eval-spec

## 12. M3-Corpus (separate milestone, for context only)

M3-Corpus is the companion milestone separated from M3-Eval per locked
decisions 2 and 4. It is **not** in scope here; this section is informational
for the deliver agent's M3 candidate selection round.

**Likely scope of M3-Corpus** (one sub-sprint estimated):

- `S-Corpus-1`: per-UC FAQ corpus audit; closes
  `R-corpus-coverage-audit-per-uc`; samples user entry-point queries per UC;
  validates `docs/FAQ-knowledge_include_help_url.csv` coverage for
  resolve-grade articles; explicitly forbids generator-synthesised articles;
  migrates real Help Centre content where coverage gaps surface.

**Estimated**: 5-7 audit days + curation. Can run in parallel with any of
the M3-Eval sub-sprints under a separate dev / review path.

## 13. Deliverables for the deliver agent

Upon human confirmation of this proposal (already given 2026-05-20 per §4),
the deliver agent's next steps are:

1. **Pre-flight (one-time setup)**:
   - Archive existing `docs/milestone_objective.md` (M2, closed 2026-05-18)
     to `docs/milestones/M2_objective.md` if not already done.
   - Refresh `docs/10-handoff.md` §1 lead: demote M2 to "Preceding
     milestone", set M3-Eval as "Current milestone".

2. **Author `docs/milestone_objective.md` (M3-Eval)**:
   - Follow `iteration_governance.md` §8.3 schema (front matter +
     11 body sections).
   - Populate §3 "Sub-sprint sequence" from §6 of this proposal (5
     sub-sprints in dependency order).
   - Populate §5 "Milestone acceptance bar" from §8 of this proposal.
   - Populate §6 "Hard fences" from §9 of this proposal.
   - Populate §7 "R-items consumed / surfaced" from §11 of this proposal.
   - Populate §8 "Codex review plan" from §7 of this proposal
     (milestone-shared default, per-sub-sprint trigger on S-Eval-3).

3. **Author `docs/sprint_objective.md` (S-Eval-1)**:
   - Use S-Eval-1 scope from §6.
   - Use the §7 stanza pre-fill from §6 verbatim, then refine per
     `iteration_governance.md` §7 if the deliver agent's read of the code
     requires adjustment.

4. **Prompts**:
   - Draft dev agent prompt for S-Eval-1 (Claude Code as dev).
   - Draft review agent prompt for S-Eval-1 close (Codex with §4.1
     nine-question kernel + §4.2 sprint-close header).

5. **Mid-milestone rolling**: as each sub-sprint closes, the deliver agent
   advances to the next sub-sprint's contract per the dependency order in
   §10.2. S-Eval-3 requires Codex per-sub-sprint review before S-Eval-4
   begins.

6. **Milestone close (M3-Eval)**: per `iteration_governance.md` §8.4, the
   deliver agent + human produce milestone closure artefacts (verdict
   update, archive to `docs/milestones/M3_eval_objective.md`, action_bank
   §6.5 closed milestone row, handoff refresh, M3 candidate selection
   for next milestone).

## 14. References

### 14.1 Files cited (source-of-truth claims)

- `eval_interactive/results/20260518-091559/results.json` — smoke run with
  `llm_transport_error` motivating §5.5 demotion
- `eval_interactive/eval_interactive/case_spec/schema.py` — current
  `Expected` / `CaseSpec` schema
- `eval_interactive/eval_interactive/scoring/hard_checks.py` — L1 checks
- `eval_interactive/eval_interactive/scoring/outcome_checks.py` — L2 checks
- `eval_interactive/eval_interactive/scoring/llm_judge.py` — L3 judge
- `eval_interactive/eval_interactive/scoring/composite.py` — composite
  scoring rules
- `eval_interactive/case_specs/bad_cases/_manifest.md` — bad-case lifecycle
  ledger
- `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` —
  current single bad case
- `eval_interactive/case_spec_overrides.yaml` — 29 approved override
  entries; source for S-Eval-4 bad-case selection
- `server/src/main/resources/skills/` — M2-landed Skill YAML directory
  (6 Skills)
- `server/src/main/resources/config/tool-policy.yaml` — per-UC tool gating
- `docs/current/iteration_governance.md` §1 (Constitution), §3 (Fix Layer
  Classification), §4 (Codex review), §5 (Eval Acceptance Rules; §5.5
  smoke demotion; §5.6 bad-case suite primary gate), §7 (sprint-objective
  stanza), §8 (milestone framework)
- `docs/current/runtime_contract.md` — current delivered runtime contract
- `docs/current/faq_grounding_contract.md` — §L1 / §L2 grounding contract
- `docs/proposals/skill_registry_design.md` — M2 design freeze (now
  delivered)
- `docs/runtime_freeze_and_risk_policy.md` — Tier-0 invariants
- `docs/release_gate.md` — production launch blocker ledger
- `docs/foundational/phase5_evaluation_design.md` — original eval design
  (predates §5.5/§5.6 demotion)

### 14.2 Cross-milestone context

- `docs/milestones/M1_objective.md` (DISCOVER + Intake; closed 2026-05-17)
- `docs/milestone_objective.md` (M2 Skill Registry; closed 2026-05-18) —
  archive to `docs/milestones/M2_objective.md` as part of M3-Eval pre-flight
- `docs/teams/collaboration-guide.md` §3 — Path 1
  vs Path 2 narrative; this proposal is Path 1 (research-driven)
