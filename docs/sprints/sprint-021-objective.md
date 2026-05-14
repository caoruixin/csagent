---
title: Sprint 21 — Wave A5/A6 L3 Review Batch (per-case + 1 systematic)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-13
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 21 is the Wave A5/A6 L3 review batch named in Sprint 20 handoff
  §12 / §11 Q2. Single-track. Processes 6 per-case L3 R-items + 1
  systematic R-item that spans 3 UCs. Sprint class: semantic-touching
  (the eval_spec surface is the primary target); the §7 stanza is
  REQUIRED. Each R-item resolves to one of three dispositions —
  approved L3 override (eval_spec), rejected as non-eval_spec (layer
  reclassification + new R-item), or deferred (open with reason). The
  cs_095 disposition has a cascade risk on the Sprint 20-authored
  cs_095 case family; the sprint explicitly defers the family refresh
  to a follow-on R-item even if cs_095's L3 review reverses the UC
  primary. The §1.7 forbidden-line ("widening eval spec to accept a
  genuine bot mistake") governs hard; for every approved override the
  dev agent must show evidence that the bot's behaviour was correct
  and the CaseSpec / rubric was wrong, not the other way around.
---

# Sprint Objective

Date: 2026-05-13

## Sprint name

Sprint 21 — Wave A5/A6 L3 Review Batch (per-case + 1 systematic)

## Goal

Process 7 L3 R-items surfaced by the Sprint 18 G1 Failure Briefs and
held over in `docs/action_bank.md` §5.2 as the Wave A5/A6 L3 review
backlog. For each R-item, deliver an **L3 disposition** in one of
three classes:

- **Approved override (eval_spec adjustment).** The bot's behaviour
  was correct; the CaseSpec or judge rubric was wrong. Append /
  update `eval_interactive/case_spec_overrides.yaml` (schema v2,
  keyed by `source_session_id`) with the override, with a clear
  `rationale:` (or `reason:`) field that cites the source brief +
  the Constitution clause that justifies the override.
- **Rejected as non-eval_spec.** On L3 review, the R-item is not an
  eval_spec problem — the brief's original layer hypothesis was
  misclassified at G1, or new evidence points to a different layer
  (commonly `prompt_projection` or `semantic_planner`). The dev
  agent documents the rejection in the handoff with the new layer
  hypothesis and proposes a new R-item in §12 for the deliver agent
  to apply.
- **Deferred with reason.** The disposition needs more evidence or a
  product / policy decision the dev agent cannot make. Documented in
  the handoff; the R-item stays open in `action_bank.md` with the
  new reason carried forward.

Sprint 21 is a **single-track, semantic-touching sprint** on the
eval_spec surface (with potential per-R-item reclassification to
`judge_calibration` or `semantic_planner`). The §7 stanza is
REQUIRED.

## Background

Sprint 20 closed (commit `dfbbf04`) with two deliverables shipped:
the 10 G1 case families (Track A) and the `already_called` soft-
signal projection slot (Track B). Sprint 20 handoff §11 Q2 raised
the cs_095 cascade risk and §12 enumerated the L3 review batch as
the next recommended sprint after Sprint 20. Sprint 21 picks up
that recommendation. No remediation of any brief's underlying
failure happens in Sprint 21; the work is L3 disposition only —
classification of each R-item into the three classes above, with
the artefact for approved entries being a YAML append /update to
`case_spec_overrides.yaml`.

The L3 review batch is the cleanest next sprint because:

- The 6 per-case L3 R-items are all tagged "Wave A5/A6 L3 review"
  in `docs/action_bank.md` §5.2 and the source briefs already
  contain the evidence the dev agent needs (Ground-truth chain
  preamble + What happened / What should + Why this matters).
- The 1 systematic R-item
  (`R-generator-get-customer-context-policy-mismatch`) spans 3 UCs
  (UC-C / UC-D / UC-F) and is sized for one sprint of disposition
  work; it may resolve to a single shared override, three
  coordinated per-case overrides, or layer reclassification.
- No runtime change, no prompt change, no FAQ corpus change, no
  judge code change in scope. The single semantic surface touched
  is `case_spec_overrides.yaml`. (One narrow exception: judge rubric
  edits in `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`
  are allowed **only** if a disposition explicitly classifies as
  `judge_calibration` AND the dev agent's §1.7 self-check passes —
  see "Do not implement" for the §1.7 fence.)

## R-item list (7 total — in execution order)

1. **`R-cs001-escalation-trigger-l3-review`** (per-case). Source
   brief: `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md`.
   Question: does the CaseSpec's `escalation_trigger:
   clarification_budget_exhausted` reconcile with `intake fields
   (none)`? Disposition options: approved override (correct the
   trigger / clear it); rejected (the contradiction is real and the
   bot's escalate was wrong → layer = `semantic_planner` /
   `prompt_projection`); deferred.
2. **`R-cs038-l3-review-intake-efficiency`** (per-case). Source
   brief: `docs/diagnostics/failure-briefs/cs038-uc-j-intake-redundancy-and-jargon-framing.md`.
   Question: should intake completion at T2 be the correct
   `turn_efficiency` target? Disposition: approved override
   (relax the target); rejected (the bot's mechanical re-asking
   is a real `semantic_planner` failure); deferred.
3. **`R-cs040-l3-review-intake-completion-semantics`** (per-case).
   Source brief: `docs/diagnostics/failure-briefs/cs040-uc-k-disengaged-jargon-intake-false-complete.md`.
   Question: should `escalation_reason=intake_complete_for_uc_k` +
   `intake_fields_collected=0` be a hard outcome fail? Disposition:
   approved override (CaseSpec was too strict); rejected (this is
   the Tier-0-candidate `R-escalation-reason-runtime-evidence-contract-review`
   territory and belongs in `java_guard` / `human_review_required`,
   not eval_spec); deferred.
4. **`R-cs095-uc-classification-l3-rereview`** (per-case — highest
   impact). Source brief: `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md`.
   Question: PRD/Eval expected UC-D vs the approved Wave A2.1
   legacy override pinning UC-A primary — which classification is
   the true ground truth? Disposition: approved override (rewrite
   the classification block to UC-D primary); rejected (the legacy
   override is right; PRD/Eval was wrong, but the human re-review
   stands as a documented disagreement → new R-item for product
   /policy review); deferred. **Cascade rule: see "Do not
   implement" — the dev agent MUST NOT touch the Sprint 20-authored
   cs_095 case family content under any circumstance.**
5. **`R-cs176-escalation-reason-l3-review`** (per-case). Source
   brief: `docs/diagnostics/failure-briefs/cs176-uc-e-wrong-escalation-reason-family.md`.
   Question: is `user_requested` the right expected reason for UC-E
   refund demand, or should UC-E carry a more specific reason?
   Disposition: approved override (refine the expected reason);
   rejected (the bot picked the wrong family of reason; this is
   `semantic_planner`); deferred.
6. **`R-cs192-secondary-ucs-duplicate-uc-b`** (per-case — low
   impact). Source brief: `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md`.
   Question: CaseSpec lists UC-B both as primary and as
   `secondary_ucs`; is this a generator quirk or intentional?
   Disposition: approved override (deduplicate); rejected
   (intentional — and explain why); deferred.
7. **`R-generator-get-customer-context-policy-mismatch`**
   (systematic — spans cs_001 / cs_011 / cs_259). Source briefs:
   `cs001-uc-c-template-escalate-on-faq-miss.md`,
   `cs011-uc-d-detailed-description-ignored-on-faq-miss.md`,
   `cs259-uc-f-sprint7-i0-violation-on-payment-question.md`.
   Question: the CaseSpec generator includes `get_customer_context`
   in `expected_tool_sequence` for UC-C / UC-D / UC-F despite
   phase 2 §2.10 line 358 restricting that tool to UC-A / UC-FP /
   UC-K. Disposition: approved override(s) (remove
   `get_customer_context` from each affected CaseSpec's expected
   sequence — 3 coordinated per-case overrides, or one shared
   template override if such a thing exists in the schema);
   rejected (the layer is `prompt_projection` or
   `system_prompt.txt` — the bot's policy understanding of when
   `get_customer_context` is allowed is the root cause, not the
   CaseSpec → propose a new R-item targeting that layer);
   deferred.

## Layer-classification + anti-hardcode stanza

**Target failure layer:** **multi-layer prospective per R-item.**
The sprint's primary layer is `eval_spec` (each L3 disposition
either approves an eval_spec adjustment via
`case_spec_overrides.yaml` or rejects the R-item as belonging to a
different layer). Secondary candidates per disposition:
`judge_calibration` (if an L3 rubric edit in
`llm_persona_reviewer.py` is the right fix), `semantic_planner`
(if the bot's choice was wrong and the CaseSpec was right),
`prompt_projection` (if the bot lacked the right context to make
the choice), or `human_review_required` (if the disposition
requires product / policy sign-off — most likely for the cs_040
intake-complete or cs_095 PRD-vs-override disagreement). The dev
agent walks each R-item independently and classifies. The stanza
shape follows the `feedback_multi_layer_prospective_stanza`
precedent established in Sprint 19 and Sprint 20.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. If any
R-item disposition would require a new Tier-0 (e.g. cs_040's
`intake_complete_for_uc_k` with zero collected fields reads like
Tier-0 territory), the dev agent flags `human_review_required` per
`iteration_governance.md` §3.2 Q2 and stops. The Tier-0-candidate
R-item `R-escalation-reason-runtime-evidence-contract-review` is
already on the action_bank backlog and is **out of scope** for
Sprint 21 (the disposition for cs_040 / cs_176 here is about the
CaseSpec, not the runtime contract).

**Semantic hardcode:** No semantic hardcode introduced. Approved
overrides on `case_spec_overrides.yaml` describe per-case ground
truth (the human-reviewed correct answer for one specific
`source_session_id`); they are NOT keyword / regex / if-else /
enum / per-UC matrix rules in runtime or prompt. The override file
is consumed by the eval harness, not by the agent. If any
disposition tempts a judge rubric tightening in
`llm_persona_reviewer.py` that adds a keyword / regex / if-else
branch on user content or tool output, that is a `semantic
hardcode` and is **forbidden** — the dev agent must propose the
fix at a different layer (typically `prompt_projection` or
`semantic_planner`) or defer.

**Generalization coverage:** target = each of the 7 R-items'
specific `source_session_id`(s); for the systematic R-item, target
= 3 source session ids (cs_001, cs_011, cs_259). Neighbor =
existing Sprint 20-authored case-family neighbor specs (e.g.
cs001 family neighbors at `eval_interactive/case_specs/case_families/cs001_uc_c_template_escalate/`).
Negative = existing Sprint 20-authored family negatives.
Shadow = existing Sprint 20-authored family shadows (visible
to human and review agent; NOT consumed by dev). For each
approved override, the handoff must state: "this override holds
across the family's neighbors and does not regress the negatives"
with a path reference. For the systematic R-item, coverage
spans cs_001, cs_011, cs_259 families simultaneously.

## Do not implement

- **Editing Sprint 20-authored case family content under any
  circumstance.** Specifically, all paths under
  `eval_interactive/case_specs/case_families/**` (target /
  neighbor / negative subdirectories) and all paths under
  `eval_interactive/case_specs_shadow/case_families/**`. The
  Sprint 20 cs_095 family was authored assuming UC-D primary as
  the correct classification; if the cs_095 L3 re-review (R-item
  #4 above) reverses the UC primary to UC-A, the family's
  expected fields would need a refresh — that refresh is **NOT**
  in Sprint 21 scope. Sprint 21 produces the L3 disposition only;
  the family refresh becomes a new R-item
  (`R-cs095-family-refresh-post-l3-reversal`) the deliver agent
  applies in §12 at close.
- **Widening any judge rubric to accept the bot's actual output
  when the bot was genuinely wrong.** This is the
  `iteration_governance.md` §1.7 forbidden line ("widening eval
  spec to accept a genuine bot mistake"). For every approved L3
  override, the dev agent must explicitly cite, in the
  disposition write-up, evidence from the source brief
  (Ground-truth chain preamble + What happened + What should)
  that the bot's behaviour was correct on that specific
  `source_session_id` and the CaseSpec / rubric was the artefact
  that needed adjustment. Absent that evidence, the disposition
  must be `rejected` or `deferred`, not `approved`.
- **Editing any smoke / anchor / promotion / exploration
  CaseSpec.** Specifically: all paths under
  `eval_interactive/case_specs/smoke/*`,
  `eval_interactive/case_specs/anchor/*`,
  `eval_interactive/case_specs/promotion/*`,
  `eval_interactive/case_specs/exploration/*`. Smoke CaseSpec
  edits are the §1.7 forbidden surface — overrides go in
  `case_spec_overrides.yaml`, not inline in the CaseSpec YAML.
- **Editing prompt files, server runtime, FAQ corpus, sprint
  archives, governance docs.** Specifically: no edits to
  `server/**`, `data/**`, `FAQ-knowledge_include_help_url.csv`,
  `eval_interactive/personas*.yaml`, `eval_interactive/data/**`,
  `docs/sprints/**`, `docs/archive/**`,
  `docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`,
  `docs/runtime_freeze_and_risk_policy.md`,
  `docs/foundational/**`, `AGENTS.md`, `CLAUDE.md`.
- **Re-running smoke beyond verifying that an approved override
  picks up correctly.** Sprint 21 is an L3 disposition sprint, NOT
  a smoke remediation sprint. If the dev agent wants to confirm
  that a new override entry is parsed and applied as expected, a
  one-shot dry run of the eval harness against a single case is
  acceptable as a sanity check; full smoke reruns are NOT.
- **Opening a new Tier-0 invariant in
  `docs/runtime_freeze_and_risk_policy.md`.** Any disposition that
  reads like Tier-0 territory must surface as
  `human_review_required` (per §3.2 Q2 / §3.2 default tail), with
  the new layer hypothesis and a proposed action_bank entry in §12.
- **Touching any of the other R-items in `docs/action_bank.md`
  §5.2 not named in this objective.** Specifically, the Tier-0-
  candidate `R-escalation-reason-runtime-evidence-contract-review`,
  the systematic `R-l3-judge-form-context-trust-rubric`,
  `R-corpus-coverage-audit-per-uc`,
  `R-faqMissCount-threshold-and-timing-review`,
  `R-duplicated-greeting-projection-fix`,
  `R-uc-b-customer-context-policy-review`,
  `R-l1-source-citation-quality-rubric`,
  `R-runtime-orchestrator-tool-call-deduplication`,
  `R-smoke-regression-investigation`,
  `R-already-called-prompt-consumption`,
  `R-persona-goal-summary-scope-clarity`,
  `R-g2-multi-turn-followup-case-family-design`. They are all
  explicitly out of Sprint 21 scope.
- **Editing `docs/sprint_objective.md`, `docs/codex-findings.md`,
  or any file under `docs/sprints/`.** The dev agent writes
  `docs/sprints/sprint-021-handoff.md` (NEW) and refreshes
  `docs/10-handoff.md`. The deliver agent archives the sprint
  objective at close.
- **Architecture-health metric collection.** Per `iteration_governance.md`
  §6, the four metrics remain `collection_status: not_started`.

## Success metrics (per `iteration_governance.md` §5.1)

- **Target deliverable.** 7 L3 dispositions delivered (one per
  R-item). Each disposition is classified as `approved` /
  `rejected` / `deferred` with explicit citation to the source
  brief and (for approved overrides) the §1.7 self-check
  evidence.
- **Approved overrides land in `case_spec_overrides.yaml`.**
  Each approved entry is keyed by `source_session_id`, declares
  the relevant blocks (`classification` / `expected` / `persona`),
  carries `status: approved`, `reviewer:`, `date:`, `confidence:`,
  `source:`, and a `rationale:` (or `reason:`) field that names
  the source brief path + the Constitution / governance clause
  justifying the override.
- **Rejected R-items produce a new R-item proposal.** For each
  rejection, the handoff §12 includes the new layer hypothesis +
  a proposed action_bank entry naming the alternate layer (e.g.
  `R-cs040-intake-complete-semantic-planner-review` if the cs_040
  disposition is rejected as not eval_spec).
- **Deferred R-items carry forward with a reason.** The handoff
  §12 names the new evidence or product / policy decision that
  must arrive before the deferral can lift.
- **§1.7 self-check passes for every approved override.** The
  handoff §7 (anti-hardcode self-walk) walks each approved
  override against §4.1 Q4 and §5.4, with the source-brief
  evidence quoted or cited.
- **Generalization-coverage statement per approved override.**
  For each approved override, the handoff names the corresponding
  Sprint 20 case family and states that the override is
  consistent with the family's neighbors / negatives / shadow.
- **Neighbor / negative no regression.** No Sprint 20-authored
  family content is edited (per "Do not implement"), so by
  construction the neighbor / negative / shadow specs are
  unchanged.
- **Safety floor unchanged.** Tier-0 safety invariants (PII,
  safety, identity verification, imminent harm) remain green.
  L3 disposition is paper-only; no runtime change.
- **Grounding floor unchanged.** FAQ grounding diagnostics per
  `docs/current/faq_grounding_contract.md` remain at or above
  prior level.
- **Wrong-containment rate unchanged or down.**
- **Over-escalation rate unchanged or down.**
- **Architecture-health metrics not regressed.** §6 metrics
  remain `collection_status: not_started`.

## Review rule

Codex runs the Anti-Hardcode Review (`iteration_governance.md`
§4.1) on the Sprint 21 commits and writes its decision to
`docs/codex-findings.md` using the §4.2 sprint-close 4-line
header.

Codex is told explicitly (in the review prompt at
`compact/sprint-021-review-prompt.md`) that:

- L3 disposition work on `case_spec_overrides.yaml` is the
  sprint's deliverable, **not scope drift**. Per-R-item
  classification (approved / rejected / deferred) is the
  expected output shape.
- The §1.7 forbidden line is enforced HARD. Any approved
  override that masks a genuine bot mistake is **blocking**.
  Codex's job for each approved override: verify the dev
  agent's evidence that the bot was correct on that specific
  `source_session_id` and the CaseSpec / rubric was wrong.
- **The cs_095 cascade rule is non-negotiable.** Any edit by
  the dev agent to Sprint 20-authored case-family content (any
  path under `eval_interactive/case_specs/case_families/**` or
  `eval_interactive/case_specs_shadow/case_families/**`) is
  **blocking** as scope drift / governance violation.
- The deliver-agent-owned files that may be in the working
  tree at commit time (`docs/sprint_objective.md` Sprint 21
  objective, `compact/sprint-021-*-prompt.md`,
  `compact/sprint-deliver-orchestrator.md`) are NOT scope
  drift. They are deliver-agent operational surface and will be
  rolled forward at close. Per
  `feedback_out_of_scope_review_packaging_rollforward.md`, if
  the only blocking finding would be a commit-boundary
  observation on those files, return `approve` with a
  one-line packaging note in the §4.2 header summary.
- Out-of-scope concerns (opinions on the other R-items, on
  G3+ remediation order, on the smoke regression, on the
  systematic R-item's right resolution beyond what the dev
  agent proposed) are recorded in `docs/action_bank.md` as
  deferred items, NOT as blocking findings.

`fix_required` is reserved for concrete §4.1-failing findings on
the Sprint 21 commits: an approved override that masks a bot
mistake (§1.7); an edit to Sprint 20-authored case-family
content (cascade rule); a judge rubric edit that introduces a
keyword / regex / if-else hardcode; a disposition recorded as
`approved` without the required §1.7 self-check evidence; a
new Tier-0 invariant silently opened in
`docs/runtime_freeze_and_risk_policy.md`.

## Deliverables (what the dev-agent commits must contain)

- 7 L3 disposition write-ups in
  `docs/sprints/sprint-021-handoff.md` (NEW) — one per R-item,
  classified as approved / rejected / deferred, with the source
  brief citation, the disposition evidence, and (for approved)
  the §1.7 self-check evidence + the YAML diff applied.
- `eval_interactive/case_spec_overrides.yaml` — append / update
  entries for each approved override. Each entry uses the
  schema v2 shape (lookup by `source_session_id`; required
  fields `status: approved`, `source:`, `reviewer:`, `date:`,
  `confidence:`, `rationale:`; blocks `classification` /
  `expected` / `persona` as relevant).
- (Conditional, allowed only with explicit §1.7 self-check
  pass) `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`
  judge rubric edit, ONLY if a disposition classifies as
  `judge_calibration` and the rubric edit does not introduce a
  keyword / regex / if-else / per-UC matrix branch.
- `docs/10-handoff.md` refreshed to lead with Sprint 21.
- No edit to `docs/sprint_objective.md` (the deliver agent
  archives at sprint close); no edit to any file under
  `docs/sprints/` other than the new Sprint 21 handoff; no
  edit to `docs/codex-findings.md` by the dev agent (Codex
  writes that file at sprint close).

The dev-agent commit pattern is **commit-at-end on branch
`design-v1-without-human-review`**. No worktree, no PR.

Codex writes its review to `docs/codex-findings.md` after the
dev commits land on `design-v1-without-human-review`, per the
review prompt at `compact/sprint-021-review-prompt.md`.

---

## Sprint 21 fix iteration

Date: 2026-05-13

### Trigger

Codex review at `docs/codex-findings.md` returned
`decision: fix_required` / `blocking_count: 3`. All three blocking
findings are **approved-override evidence-gap** findings on the
same shape: each of the three approved overrides (cs_001, cs_095,
cs_192) is missing the verbatim brief-field quotes that the parent
Sprint 21 review prompt made blocking under the §1.7 evidence-
package gate. Dev quoted Ground-truth chain only; the gate
required Ground-truth chain **plus** the brief's `What happened?`
field **plus** the brief's `What should a good CS agent have
done?` field. See `docs/sprint_objective.md` lines 237–243 ("Do
not implement") and lines 318–321 (Success metrics) above for the
parent-sprint requirement.

### Findings to close (lines per `docs/codex-findings.md`)

- **Finding 1 — cs_001** (`docs/codex-findings.md` lines 8–26).
  Target: `docs/sprints/sprint-021-handoff.md` line 270 §1.7
  self-check paragraph (R-cs001-escalation-trigger-l3-review
  approved). Brief lines to quote verbatim:
  `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md:18`
  (`What happened?` field) and
  `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md:24`
  (`What should a good CS agent have done?` field).
- **Finding 2 — cs_095** (`docs/codex-findings.md` lines 28–47).
  Target: `docs/sprints/sprint-021-handoff.md` line 431 §1.7
  self-check paragraph (R-cs095-uc-classification-l3-rereview
  approved). Brief lines to quote verbatim:
  `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md:27`
  (`What happened?` field) and
  `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md:50`
  (`What should a good CS agent have done?` field).
- **Finding 3 — cs_192** (`docs/codex-findings.md` lines 49–67).
  Target: `docs/sprints/sprint-021-handoff.md` line 576 §1.7
  self-check paragraph (R-cs192-secondary-ucs-duplicate-uc-b
  approved). Brief lines to quote verbatim:
  `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md:22`
  (`What happened?` field) and
  `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md:41`
  (`What should a good CS agent have done?` field).

### cs_095 dimension-distinction clause (load-bearing)

The cs_095 §1.7 self-check at handoff line 431 already explicitly
distinguishes the dimension being overridden (UC classification —
where the bot's user-facing CONTENT was correct UC-D) from the
orthogonal dimensions where the bot was genuinely wrong (skipped
account-state investigation, duplicated greeting "Hi Trish! Hi
Trish", internal SF source_id leak `ka44J000000gKxqQAE`, T1
PLACEHOLDER_WITHOUT_FOLLOWUP stall). The handoff explicitly says
those orthogonal failures "remain real bot bugs at
`prompt_projection` / `semantic_planner` that this eval_spec
override does NOT widen eval to accept" (handoff lines 442–450)
and notes "the 2026-05-05 run still hard-fails composite 0.0 on L1
no_stall regardless of UC classification" (handoff lines 449–450).

**The fix iteration MUST preserve that dimension distinction.**
The brief-quote insertion is paste-in only; it does not soften,
remove, or restructure the existing dimension-distinction
language. If the dev agent finds itself rewriting that paragraph
to fit the quotes in, that is a stop-condition — quote insertion
must be additive, not substitutive.

### Layer-classification + anti-hardcode stanza (fix-iteration form)

**Target failure layer:** `eval_spec` (same as parent Sprint 21
stanza). All three blocking findings are evidence remediation on
the disposition write-up surface. No layer reclassification.

**Tier-0 invariant:** This fix iteration adds no Tier-0
invariant.

**Semantic hardcode:** No semantic hardcode introduced. This is
documentation evidence-gap remediation: pasting verbatim quotes
from the source briefs into the handoff §1.7 self-check
paragraphs. No runtime change, no prompt change, no judge rubric
change, no override YAML change.

**Generalization coverage:** unchanged from parent Sprint 21
stanza. The three target source_session_ids (cs_001:
`570Q5000008kr6LIAQ`; cs_095: `570Q5000008U5C9IAK`; cs_192:
`570Q5000008rbcjIAA`) are unchanged. No new neighbor / negative /
shadow coverage is opened or closed by this fix.

### Do not implement (fix-iteration scope fence)

- **Do not edit `eval_interactive/case_spec_overrides.yaml`.** The
  substantive overrides for cs_001, cs_095, cs_192 are correct per
  Codex's non-blocking checks (`applied: 17`, `pending: 0`, three
  Sprint 21 source_session_ids present). Touching the YAML is a
  scope drift / re-litigation.
- **Do not edit any case-family content.** All paths under
  `eval_interactive/case_specs/case_families/**` and
  `eval_interactive/case_specs_shadow/case_families/**` remain
  fenced per the parent Sprint 21 cascade rule.
- **Do not edit the judge rubric** at
  `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`.
- **Do not edit any other R-item's disposition write-up in the
  handoff.** Only the three §1.7 self-check paragraphs at lines
  270, 431, 576 are in scope. The other 4 R-item dispositions
  (cs_038 deferred, cs_040 deferred, cs_176 rejected,
  R-generator-get-customer-context-policy-mismatch rejected) are
  untouched.
- **Do not soften the cs_095 dimension distinction.** See clause
  above. Quote insertion must be additive.
- **Do not re-litigate the substantive override decisions.** All
  three approved overrides remain approved; the fix is purely
  evidence-package completion.
- **Do not edit `docs/sprint_objective.md`** (this file — the
  deliver agent owns it) **or `docs/codex-findings.md`** (the
  review agent re-writes the header at fix re-review).
- **Do not edit prompt files, server runtime, FAQ corpus, sprint
  archives, governance docs, or foundational docs.** All
  fences from the parent Sprint 21 objective hold.

### Success criteria (fix iteration)

- Three §1.7 self-check paragraphs at `docs/sprints/sprint-021-handoff.md`
  lines 270, 431, 576 each contain TWO new verbatim quotes (six
  new quotes total), one each from the brief's `What happened?`
  field and `What should a good CS agent have done?` field, with
  a path:line citation for each quote.
- Quotes are verbatim (italics permitted, paraphrasing not).
- The cs_095 dimension distinction (handoff lines 431–450) is
  intact word-for-word — only additive quote insertion is made.
- No file outside `docs/sprints/sprint-021-handoff.md` is modified
  (and within that file, only the three §1.7 self-check paragraphs
  plus a small `## Fix iteration` section appended at the bottom).
- The fix handoff section names the three findings closed,
  exhibits the six new quote blocks, and confirms the cs_095
  dimension distinction held.

### Review rule (fix iteration)

Codex re-reviews ONLY the fix bundle (`HEAD^..HEAD` after the
fix commit). Per-finding resolution check (verbatim quote
present? dimension distinction preserved on cs_095?) plus a
substantive re-verification that
`eval_interactive/case_spec_overrides.yaml` is unchanged for the
three cases. Codex replaces the `## Sprint Review Decision`
header at the top of `docs/codex-findings.md` in place with
`## Sprint Review Decision (Sprint 21 fix re-review)` + the
§4.2 4-line header. Packaging-rollforward rule applies per
`feedback_out_of_scope_review_packaging_rollforward.md` —
deliver-agent-owned uncommitted files in the working tree are
NOT blocking.

### Deliverables (fix-iteration commits)

- `docs/sprints/sprint-021-handoff.md` — three §1.7 self-check
  paragraph edits (additive quote insertion only) + a
  `## Fix iteration` section appended at the bottom of the file
  naming the three findings closed.
- No other files modified by the dev agent. The fix-iteration
  objective (this section) is deliver-agent-owned and not staged
  by dev.

The fix-iteration commit pattern is **commit-at-end on branch
`design-v1-without-human-review`**. No worktree, no PR. Expect
the working tree to contain deliver-agent-owned uncommitted files
(`compact/sprint-021-fix-*.md`, this `sprint_objective.md`
append) per `feedback_commit_at_end_bundles_deliver_artefacts.md`;
the fix-dev prompt directs the dev agent to NOT stage these.
