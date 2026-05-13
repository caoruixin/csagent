# Sprint 21 Handoff — Wave A5/A6 L3 Review Batch (per-case + 1 systematic)

Date: 2026-05-14
Branch: `design-v1-without-human-review`
Sprint class: single-track, semantic-touching on the eval_spec surface.
Per `docs/sprint_objective.md` (Sprint 21), the §7 stanza is REQUIRED;
this handoff fulfils it in multi-layer prospective form per the
Sprint 19 / Sprint 20 precedent.

## 1. Context Pack

Produced before any YAML write per `docs/current/agent_context_guide.md`
Context Pack Prompt. Pasted in chat 2026-05-13 (Sprint 21 plan day),
human confirmed dispositions on 2026-05-14 (Q1 ▸ Path B widening UC-C +
UC-D + UC-F; Q2 ▸ approved with tightening; subsequent schema
constraint surfacing flipped cs_038 + cs_040 to deferred).

**Relevant docs.**

- `AGENTS.md` (durable-connective; current) — constitution chain root.
- `docs/current/doc_governance.md` (durable-connective; current) —
  front-matter schema; this handoff is `current-runtime` per-sprint.
- `docs/current/agent_context_guide.md` (durable-connective; current) —
  per-task reading list; task type "Eval, governance".
- `docs/current/iteration_governance.md` (durable-connective; current) —
  §1 Constitution (§1.5 Iteration rule, §1.7 Forbidden line), §3 Fix
  Layer Classification, §4.1 Anti-Hardcode 9-question prompt, §5
  Eval Acceptance Rules (§5.4 no eval-side override of a real bug),
  §7 sprint-objective stanza. **§1.7 is load-bearing for every
  approved override in §3 below.**
- `docs/sprint_objective.md` (current-runtime; current; 2026-05-13) —
  authoritative scope. Sprint 21 = 7 L3 R-items, dispositions
  {approved / rejected / deferred}, hard fences on case-family
  content, smoke / anchor / promotion / exploration CaseSpec inline
  edits, prompts, server runtime, judge code (except `judge_calibration`
  with §1.7 self-check), foundational docs, sprint archives, governance
  docs.
- `docs/sprints/sprint-018-handoff.md` (sprint-archive; archived) — §5.1
  Method note: L3 override pipeline schema v2; lookup by
  `source_session_id` (NOT `case_id`); approved entries require
  `status: approved`, `source:`, `reviewer:`, `date:`, `confidence:`,
  `rationale:`; `supporting_turn_numbers` may be empty ONLY when
  `migrated_from_legacy: true`.
- `docs/sprints/sprint-020-handoff.md` (sprint-archive; archived) —
  §3.6 cs_095 case family was authored assuming UC-D primary as the
  correct classification (the cascade direction); §11 Q2 names the
  cascade explicitly; §12 names this exact Wave A5/A6 L3 batch as the
  recommended next sprint.
- `docs/diagnostics/failure-briefs/*.md` (diagnostic; current) — read
  in full: cs001, cs011, cs038, cs040, cs095, cs176, cs192, cs259.
- `docs/action_bank.md` §5.2 (current-runtime; current) — the 7
  R-items processed by Sprint 21 are all in the §5.2 Wave A5/A6
  backlog rows.
- `eval_interactive/case_spec_overrides.yaml` (current-runtime;
  current; schema v2) — the only file appended to by this sprint.

**Relevant code paths.** Sprint 21 is paper-only on the eval_spec
surface. Code paths cited (not edited):

- `eval_interactive/case_spec_overrides.yaml` (appended in this sprint).
- `eval_interactive/eval_interactive/case_spec/schema.py` — the
  `Expected` and `ScoringConfig` dataclasses that bound what an
  override can change (load-bearing for §3.2 and §3.3 deferrals).
- `eval_interactive/eval_interactive/case_spec/extractor.py` lines
  447–500 — the override schema constants and `OverrideEntry` shape
  that gate which fields are accepted. Specifically
  `_CASE_OVERRIDE_EXPECTED_FIELDS` (line 449–461) is the load-bearing
  allowlist that does NOT include `scoring.*`, `outcome_checks`, or
  `acceptable_outcomes`.
- `eval_interactive/eval_interactive/scoring/outcome_checks.py` lines
  309–325 (`_check_turn_efficiency`) — the scorer logic that consumes
  `expected.max_turns` to compute `turn_efficiency` (the only lever
  cs_038 had access to via override).
- `eval_interactive/case_specs/smoke/cs_interactive_{001,038,040,095,
  176,192}.yaml` — base CaseSpecs read for current-state evidence
  (NOT edited per the §1.7 forbidden-surface fence).
- `eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/`
  and `eval_interactive/case_specs_shadow/case_families/cs095_uc_classification_account_aware/`
  — Sprint 20-authored cs_095 family, NOT touched per the cascade
  rule (Sprint 20 §3.6).
- `docs/foundational/phase2_domain_realization_spec.md` §2.10 line
  358 — cited for the systematic R-item disposition (the
  `get_customer_context` UC allowlist is the `product_policy` artefact
  the rejected disposition points at). NOT edited.

**Doc status warnings.**

1. The cs_095 Wave A2.1 legacy override carried `primary_uc: UC-A`
   with `migrated_from_legacy: true` and empty `supporting_turn_numbers`.
   Both Sprint 18 §5.1 (override = authoritative ground truth when
   present) and Sprint 20 §3.6 (case-family authored assuming UC-D
   primary) acknowledged the disagreement openly. Sprint 21 resolves
   it under §1.7 supervision: the §3.4 disposition write-up keeps the
   classification correction (eval_spec scope) cleanly separated from
   the bot's L1 no_stall + duplicated-greeting + skipped-tool failures
   (which remain bot bugs at `prompt_projection` / `semantic_planner`).
2. The cs_011 existing Sprint 4 §E1 override (lines 165–197 of the
   YAML at sprint open) contains `expected.bot_handling_pattern`
   wording that mentions `get_customer_context` — internally
   inconsistent with the systematic R-item's hypothesis (phase 2
   forbids the tool for UC-D). Sprint 21's disposition on the
   systematic R-item is REJECTED (route to phase 2 widening, not
   eval_spec correction), so the cs_011 entry is **not edited** in
   this sprint; the wording inconsistency lifts when
   `R-phase2-uc-cdf-customer-context-policy-widen` lands. This is
   recorded as Sprint 21 §11 open question Q1.
3. The `case_spec_overrides.yaml` schema (per
   `extractor.py:_CASE_OVERRIDE_EXPECTED_FIELDS`) is mechanically
   narrower than the Sprint 18 §5.1 Method note implied: `scoring.*`
   (outcome_checks list membership, hard_checks list membership)
   cannot be overridden, and the Expected dataclass `acceptable_outcomes`
   is also not in the override allowlist. This boundary is the
   load-bearing reason cs_038 and cs_040 deferred — see §3.3 and §3.4.

**Source-of-truth decisions.**

- For each per-case R-item: the brief's "What happened?" + "What
  should a good CS agent have done?" + Ground-truth chain preamble,
  walked against the existing override (if any) + the cited phase 2
  section + the §1.7 forbidden-line gate.
- Schema authority for new YAML entries: Sprint 18 §5.1 Method note
  + `extractor.py` lines 447–500 (the actual loader code).
- Cascade rule authority: Sprint 20 §3.6 + Sprint 20 §11 Q2 + Sprint
  21 sprint objective "Do not implement" item 1.

**Implementation status (sprint-start, verified by grep + reading).**

- 7 L3 R-items: `not_started`. None of the proposed dispositions had
  been applied before this sprint.
- L3 override pipeline schema v2: `implemented` since Sprint 18 G1 +
  Wave A6.6. 15 approved entries at sprint open; 17 at sprint close
  (cs_001 + cs_192 added new; cs_095 legacy removed; cs_095 Sprint
  21 supersession added).
- Sprint 20 cs_095 case family: `implemented` (Sprint 20 §3.6).
- `R-escalation-reason-runtime-evidence-contract-review` Tier-0
  candidate: `not_started` and OUT OF SCOPE for Sprint 21.

**Top risks before drafting dispositions (now-resolved annotations).**

1. §1.7 boundary on cs_095 (cleanest temptation: write an "approved"
   override that flips classification AND adds an `expected` block
   tied to the UC-D account-aware path, conflating the eval-spec
   correction with masking the bot's L1 stall). RESOLVED — the §3.4
   disposition keeps the two threads separate: classification flip
   ONLY; the bot's L1 no_stall + duplicated-greeting + SF source_id
   leak remain bot bugs in the existing Sprint 20 cs_095 case
   family's neighbor / negative / shadow contract. §1.7 self-check
   in §7 below.
2. cs_040 Tier-0 territory pull (tempting to fake the tightening with
   `bot_handling_pattern` wording that doesn't actually change
   scoring). RESOLVED by surfacing the schema constraint to the human
   and deferring (see §3.4); the Tier-0 candidate
   `R-escalation-reason-runtime-evidence-contract-review` is the
   broader runtime-side fix and remains in the action_bank as it was.
3. Systematic R-item three-way coordination (cs_001 / cs_011 / cs_259
   would all need coordinated approved overrides if Path A were
   chosen; the schema permits 3 separate `source_session_id`-keyed
   entries but the disposition would inherit each existing entry's
   internal consistency). RESOLVED by choosing Path B
   (`product_policy` widening; no per-case YAML coordination needed).
4. Rejected dispositions producing R-item proposals (must name a
   layer, not hand-wave). RESOLVED in §3 and §8 below.
5. Working-tree commit boundary (deliver-agent-owned files
   `docs/sprint_objective.md`, `compact/sprint-021-*.md`,
   `compact/sprint-deliver-orchestrator.md` present at sprint
   start). RESOLVED by per the §6 files-changed list — selective
   staging only.

## 2. Sprint-objective recap

From `docs/sprint_objective.md` §Goal (quoted verbatim, key clauses):

> Process 7 L3 R-items surfaced by the Sprint 18 G1 Failure Briefs and
> held over in `docs/action_bank.md` §5.2 as the Wave A5/A6 L3 review
> backlog. For each R-item, deliver an **L3 disposition** in one of
> three classes: Approved override (eval_spec adjustment) / Rejected
> as non-eval_spec / Deferred with reason.

The seven R-items processed by Sprint 21:

1. `R-cs001-escalation-trigger-l3-review`
2. `R-cs038-l3-review-intake-efficiency`
3. `R-cs040-l3-review-intake-completion-semantics`
4. `R-cs095-uc-classification-l3-rereview` (highest impact; cascade)
5. `R-cs176-escalation-reason-l3-review`
6. `R-cs192-secondary-ucs-duplicate-uc-b`
7. `R-generator-get-customer-context-policy-mismatch` (systematic;
   spans cs_001 / cs_011 / cs_259)

### Layer-classification + anti-hardcode stanza (§7)

**Target failure layer:** multi-layer prospective per R-item (the
Sprint 19 / Sprint 20 precedent for sprints whose dispositions span
more than one §3.1 layer). The seven dispositions resolve as:

- `eval_spec` (3): cs_001 approved override, cs_095 approved
  override, cs_192 approved override.
- `product_policy` (1): the systematic R-item rejected;
  layer = `product_policy`; remediation lives in a future phase 2
  widening sprint via `R-phase2-uc-cdf-customer-context-policy-widen`.
- `semantic_planner` (1): cs_176 rejected; remediation via
  `R-cs176-semantic-planner-escalation-family-discrimination`.
- `deferred pending eval_spec schema extension` (2): cs_038 + cs_040
  deferred; both blocked on the override schema not supporting
  `scoring.*` overrides; new R-item
  `R-case-spec-overrides-schema-scoring-extension` opened.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. cs_040's
disposition was specifically gated by §3.2 Q2: the reading-like-Tier-0
direction ("escalation_reason claiming session events must require
the events") is owned by the pre-existing Tier-0 candidate
`R-escalation-reason-runtime-evidence-contract-review` in
`docs/action_bank.md` §5.2 and is OUT OF SCOPE for Sprint 21. The
deferral is the correct exit per §3.2 Q2 + the sprint objective.

**Semantic hardcode:** No semantic hardcode introduced. Each approved
override is per-`source_session_id` ground truth (human-reviewed
correct answer for one specific session); none is a keyword / regex /
if-else / enum / per-UC matrix in runtime or prompt. The override
file is consumed by the eval harness at extraction time, not by the
agent.

**Generalization coverage:** target = each approved override's
specific `source_session_id` (cs_001, cs_095, cs_192). Neighbor =
existing Sprint 20-authored case-family neighbor specs (the brief's
family at
`eval_interactive/case_specs/case_families/cs001_uc_c_template_escalate/`,
`.../cs095_uc_classification_account_aware/`, `.../cs192_uc_b_giveaway/`).
Negative = existing Sprint 20-authored family negatives.
Shadow = existing Sprint 20-authored family shadows. Per the cascade
rule, no Sprint 20-authored family content is edited; the §9
generalization-coverage statement asserts each approved override
remains consistent with the Sprint 20 family contract by inspection
of the override's effect (none touch the family's expected fields
beyond what the family already expects).

## 3. Per-R-item disposition

Each sub-section walks the disposition against §1.7 self-check (for
approved overrides) or §3.2 layer reclassification (for rejected /
deferred). Source-brief evidence is quoted where the disposition
requires it.

### 3.1 R-cs001-escalation-trigger-l3-review → APPROVED override

**Source brief:** `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md`
**Source session:** `570Q5000008kr6LIAQ`
**Disposition class:** approved
**Override entry:** appended to `case_spec_overrides.yaml` after the
cs_029 entry, inside the new Sprint 21 block. `expected.escalation_trigger`
set to `faq_miss_threshold_exceeded`; `expected.bot_handling_pattern`
rewritten to match.

**Evidence walk (Ground-truth chain).** The L1-generated CaseSpec at
`eval_interactive/case_specs/smoke/cs_interactive_001.yaml` pairs
`expected.escalation_trigger: clarification_budget_exhausted` with
`expected.bot_handling_pattern` naming "intake fields (none)" (lines
28–30). This is logically contradictory: a clarification budget
cannot be exhausted when no clarification fields are required. The
brief identifies this on lines 13–15 (Ground-truth chain) and lines
63–65 (Related observation #1).

The bot's actual emitted reason on session `570Q5000008kr6LIAQ` was
`faq_miss_threshold_exceeded`. Per phase 2 §2.4 (cited by the brief),
this IS the correct reason for a UC-C case that runs `search_knowledge`
and exhausts FAQ search without a resolve-grade hit. The trace shows
the bot ran `search_knowledge` + 3 × `resolve_article` +
`request_handover` behind a turn-0 mechanical-template message.

**§1.7 self-check.** From the brief (line 14): *"The bot's actual
`escalation_reason=faq_miss_threshold_exceeded` is more semantically
accurate."* From the brief (line 16): *"The bot's outcome (escalate)
IS correct."* The override aligns the CaseSpec's expected reason
with the truthful phase 2 reason and the bot's actual correct
behaviour on this specific dimension — the CaseSpec / generator was
the artefact that needed adjustment, not the bot. This is NOT a §1.7
widening to mask a bot mistake. The bot's separate L3-quality
failure (turn-0 mechanical template, zero acknowledgment of "I am
unable to receive messages on my account") is a `prompt_projection`
/ `semantic_planner` bug captured by the Sprint 20 cs001 case family
at `eval_interactive/case_specs/case_families/cs001_uc_c_template_escalate/`
and is NOT papered over by this override.

**Artefact.** `case_spec_overrides.yaml` entry around line 333. See
§4 for the table.

### 3.2 R-cs038-l3-review-intake-efficiency → DEFERRED

**Source brief:** `docs/diagnostics/failure-briefs/cs038-uc-j-intake-redundancy-and-jargon-framing.md`
**Source session:** `570Q5000008TPw5IAG`
**Disposition class:** deferred
**Reason:** schema constraint on `case_spec_overrides.yaml`.

**Evidence walk.** The brief's R-item question (line 84): *"is intake
completion at T2 the correct `turn_efficiency` target?"* The
mechanically-clean expression of "intake should complete at T2" would
be one of: (a) override the CaseSpec's `scoring.outcome_checks` to
sharpen `turn_efficiency`, (b) introduce a new Expected field like
`expected.intake_completion_turn_target`, or (c) tighten
`scoring.hard_checks` to include an intake-stage check.

None of (a) / (b) / (c) is available via the current override
schema. `_CASE_OVERRIDE_EXPECTED_FIELDS` (extractor.py line 449–461)
allows only `{outcome_class, should_escalate, allow_bot_resolution,
bot_handling_pattern, escalation_trigger, risk_level,
expected_tool_sequence, forbidden_tools, grounding_mode,
answer_must_not_contain, max_turns}`. The only adjacent lever is
`max_turns`, which `turn_efficiency` consumes via
`scoring/outcome_checks.py:309` — but tightening `max_turns: 10 → 3`
expresses "session must end in ≤3 turns" rather than "intake state
must be detected from the user's narrative at T2". The semantic gap
is material: the test would fire false-positive on legitimate UC-J
cases that need 4–5 turns of clarification, not just on the
intake-redundancy shape cs_038 exhibits.

**§1.7 self-check (negative).** The available lever (max_turns
tightening) is the wrong shape for the failure being targeted.
Pretending the override expresses "intake-completion-from-narrative"
would mis-attribute the disposition's scope and risk catching shape
B failures unrelated to the brief.

**Disposition artefact.** No YAML write. New R-item proposed:
**`R-case-spec-overrides-schema-scoring-extension`** —
extend the override schema to permit `scoring.*` overrides (move
checks between `outcome_checks` and `hard_checks` per
`source_session_id`; alternatively, add new `Expected` fields like
`intake_completion_turn_target` that scorer logic can consume). cs_038
stays open in `docs/action_bank.md` §5.2 with this new dependency
named as the carry-forward reason.

The bot's underlying intake-redundancy + internal-jargon failures
on cs_038 remain bot bugs at `prompt_projection` (jargon framing) +
`semantic_planner` / `skill_state` (intake state detection from
narrative) per the brief's "Which layer is likely responsible?"
section (lines 62–70). Those bugs are captured by the Sprint 20
cs038 case family and are NOT papered over.

### 3.3 R-cs040-l3-review-intake-completion-semantics → DEFERRED

**Source brief:** `docs/diagnostics/failure-briefs/cs040-uc-k-disengaged-jargon-intake-false-complete.md`
**Source session:** `570Q5000008pMbOIAU`
**Disposition class:** deferred
**Reason:** schema constraint on `case_spec_overrides.yaml` (same
root as §3.2).

**Evidence walk.** The brief's R-item question (line 93): *"should
`escalation_reason=intake_complete_for_uc_k` paired with
`intake_fields_collected=0` be a hard outcome fail (not just an L2
partial score)?"* The current cs_040 CaseSpec at
`eval_interactive/case_specs/smoke/cs_interactive_040.yaml` already
lists `intake_fields_collected` in `scoring.outcome_checks` (line 66);
it contributes a partial L2 score (tool_sequence_match=0.25 per the
brief) and the composite scores 0.7531 PASS despite the bot
self-stamping `intake_complete_for_uc_k` with zero intake fields
collected.

Promoting `intake_fields_collected` from `scoring.outcome_checks` to
`scoring.hard_checks` for this one session requires either (a)
extending the override schema to permit `scoring.*` overrides, (b)
editing the smoke CaseSpec inline (which is the §1.7 forbidden
surface per the sprint objective), or (c) opening the Tier-0
candidate `R-escalation-reason-runtime-evidence-contract-review` so
the runtime enforces the evidence contract globally (out of Sprint 21
scope per §3.2 Q2 + the sprint objective's "Do not open a new Tier-0"
fence).

**§3.2 Q2 walk (sprint-objective-mandated check).** The sprint
objective for this R-item explicitly says: *"If this reads like
Tier-0 territory (runtime evidence contract), flag
`human_review_required` and stop — do NOT open a new Tier-0; the
`R-escalation-reason-runtime-evidence-contract-review` candidate is
out of Sprint 21 scope."* The reading-as-Tier-0 interpretation
applies here: the durable fix for "escalation_reason claiming session
events must require those events" is runtime-side, not per-case
eval_spec-side. The deferral is therefore the §3.2 Q2 exit, not a
silent skip.

**§1.7 self-check (negative).** A `bot_handling_pattern`-only
rewrite (the only mechanically-legal change with current schema)
documents the contract but does not move scoring — the cs_040 case
would still PASS at composite 0.7531 with the bot self-stamping
intake_complete_for_uc_k on zero fields. Calling that an "approved
tightening" would be a §5.4 eval-side override of a real bug (the
bot was wrong; the rubric was indeed too lenient; but a rubric edit
that doesn't change scoring is not actually a tightening — it's a
documentation gesture).

**Disposition artefact.** No YAML write. Shares the new R-item
**`R-case-spec-overrides-schema-scoring-extension`** with §3.2 above.
cs_040 stays open in `docs/action_bank.md` §5.2 with the same
carry-forward reason. The existing Tier-0 candidate
`R-escalation-reason-runtime-evidence-contract-review` is unaffected
and remains the broader durable fix.

### 3.4 R-cs095-uc-classification-l3-rereview → APPROVED override

**Source brief:** `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md`
**Source session:** `570Q5000008U5C9IAK`
**Disposition class:** approved
**Override entry:** the existing Wave A2.1 legacy entry (previously
in the legacy block at the file's lower portion, with `primary_uc:
UC-A`, `migrated_from_legacy: true`, empty `supporting_turn_numbers`,
and one-liner rationale "Wrong email, no adverts showing -- UC-A
with UC-D / UC-K fallback.") is SUPERSEDED by a new Sprint 21 entry
in the new Sprint 21 block. New classification: `primary_uc: UC-D`,
`secondary_ucs: [UC-A, UC-K]`. The Wave A2.1 legacy block header
comment is updated from "9 remaining" to "8 remaining (plus
cs_interactive_095 superseded by the Sprint 21 L3 re-review block
further below)".

**Evidence walk (Ground-truth chain).** The brief identifies the
disagreement on lines 6–17: PRD and Eval both classify cs_095 as
UC-D primary (account / email recovery); the Wave A2.1 legacy
override pinned UC-A primary; the bot's actual user-facing T0 reply
is UC-D-flavored (the bot answered by giving steps to change the
contact email, citing internal SF source_id `ka44J000000gKxqQAE`);
and the dominant user signal ("I think I have the wrong email
address on my app account so am not getting messages to the app
and it's telling me I have no adverts") is account-state. The
"no adverts showing" sub-symptom is a downstream consequence of the
email/account mismatch, not a standalone UC-A Top-Ad visibility
query.

The Wave A2.1 legacy entry was `migrated_from_legacy: true` with
empty `supporting_turn_numbers` — explicitly a low-evidence
inherited classification per the file's header comment lines 5–10
("supporting_turn_numbers may be empty ONLY when migrated_from_legacy:
true"). It predates the PRD/Eval convergence on UC-D the brief
documents.

**§1.7 self-check.** From the brief (line 19): *"The bot stamped
`active_use_case=UC-A` (matching the current override) but gave an
answer that reads as UC-D (steps to change the contact email)."*
The bot's USER-FACING CONTENT on UC-D-primary was correct (it
answered the UC-D question by giving email-change steps); the bot's
UC STAMP was wrong (stamped UC-A per the override but produced UC-D
content). The override was the artefact that needed adjustment, NOT
the bot's content choice on this dimension. This is the §5.4 /
§1.7-clean path: the CaseSpec / override was wrong; the eval-spec
correction aligns it with the bot's actually-correct UC-D answer.

The bot's separate L1 no_stall hard fail (T1 PLACEHOLDER_WITHOUT_FOLLOWUP
"I'm looking into this for you"), duplicated greeting ("Hi Trish!
Hi Trish"), and internal SF source_id leak on this session remain
real bot bugs at `prompt_projection` / `semantic_planner` that this
eval_spec override does NOT widen eval to accept. The brief
identifies these as five stacked failures on lines 38–48; this
override addresses only the upstream classification dimension. The
2026-05-05 run still hard-fails composite 0.0 on L1 no_stall
regardless of UC classification.

**Cascade rule (Sprint 20 §3.6 + Sprint 20 §11 Q2).** The Sprint 20-
authored cs_095 case family at
`eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/`
(target / neighbor / negative) and
`eval_interactive/case_specs_shadow/case_families/cs095_uc_classification_account_aware/`
(shadow) was authored ASSUMING UC-D PRIMARY as the correct
classification per Sprint 20 §3.6:

> *L3 override: approved Wave A2.1 legacy (source_session_id
> 570Q5000008U5C9IAK, case_spec_overrides.yaml line 311);
> classification block is UNDER L3 RE-REVIEW per
> R-cs095-uc-classification-l3-rereview (brief Related observation
> #1). For this family, the override pins UC-A primary, but the
> human re-review identifies UC-D primary; manifest records both.*

The Sprint 21 disposition CONFIRMS UC-D primary as the human-reviewed
ground truth. The cascade is satisfied IN-DIRECTION (no family
refresh needed); a future disposition that reverses UC primary again
would fire `R-cs095-family-refresh-post-l3-reversal` (recorded in §12
as a contingent future R-item; NOT opened by this sprint). No file
under `case_specs/case_families/**` or `case_specs_shadow/case_families/**`
is touched. See §5 for the cascade-rule observance checklist.

**Dependency note (carried into §11 as Q2).** The cs_095 base
CaseSpec's `expected.expected_tool_sequence` starts with
`get_customer_context`, which phase 2 §2.10 line 358 currently
restricts to UC-A / UC-FP / UC-K. With UC-D-primary classification,
the override mandates an account-aware first tool that phase 2
forbids for UC-D. The parallel Sprint 21 disposition on the
systematic R-item §3.7 proposes `R-phase2-uc-cdf-customer-context-policy-widen`
to add UC-C / UC-D / UC-F to the allowlist; until that lands, the
UC-D-primary cs_095 has an internal contract gap. This is NOT a
sprint-21-resolvable gap (phase 2 is fenced); it is named in §11 Q2
so the next sprint sequencing accounts for it.

**Artefact.** `case_spec_overrides.yaml` entry around line 411 (the
Sprint 21 block's middle entry). See §4 for the deltas table. The
Wave A2.1 legacy entry at the previous location (around line 311 of
the file at sprint open) was removed in the same edit.

### 3.5 R-cs176-escalation-reason-l3-review → REJECTED as non-eval_spec

**Source brief:** `docs/diagnostics/failure-briefs/cs176-uc-e-wrong-escalation-reason-family.md`
**Source session:** `570Q5000008NMRRIA4`
**Disposition class:** rejected
**Reason:** the CaseSpec's `expected.escalation_trigger: user_requested`
is correct; the bot was wrong; the right fix lives at
`semantic_planner`, not eval_spec.

**Evidence walk.** The CaseSpec at
`eval_interactive/case_specs/smoke/cs_interactive_176.yaml` line 32
has `expected.escalation_trigger: user_requested`. Phase 2 UC-E
policy (cited by the brief line 16) treats UC-E as a user-intent
demand category ("user paid for a service and wants the service
delivered or refunded"). The L1 `escalation_compliance` check
correctly hard-failed the bot's actual `faq_miss_threshold_exceeded`
emission on session `570Q5000008NMRRIA4` — composite dropped to 0.0
despite the bot reaching the right outcome class (escalate). The
brief identifies this on lines 32–34: cross-family escalation_reason
(user_intent vs bot_limit) is the load-bearing failure family.

**§3.2 walk for layer reclassification.**

- Q1 (infra): no infra failure on cs_176.
- Q2 (java_guard / Tier-0): a future Tier-0 candidate
  `R-escalation-reason-runtime-evidence-contract-review` exists, but
  it is OUT OF SCOPE for Sprint 21 and not opened here.
- Q3 (prompt_projection): the projection has both
  `user_requested` and `faq_miss_threshold_exceeded` in the enum the
  LLM can choose from; the user's T1 message ("Give me my £50 back
  or put my business to the top like I paid for") is an explicit
  user-intent demand that the projection surfaces unambiguously.
  Projection is adequate.
- Q4 (skill_state): cs_176 is a 2-turn case; no multi-turn flow
  state to lose.
- Q5 (semantic_planner): **the LLM had both options and chose the
  wrong family.** First-match-wins → `semantic_planner`.

**§1.7 self-check (negative, supports rejection).** Approving any
override here would mean either (a) changing
`expected.escalation_trigger` to `faq_miss_threshold_exceeded` to
match the bot's wrong choice (a textbook §1.7 widening to accept a
bot mistake) or (b) loosening L1 `escalation_compliance` to accept
cross-family reasons (a §5.4 violation). Both are forbidden.

**Disposition artefact.** No YAML write. New R-item proposed:
**`R-cs176-semantic-planner-escalation-family-discrimination`** —
the LLM must discriminate user-intent escalation families (e.g.
`user_requested`) from bot-limit escalation families (e.g.
`faq_miss_threshold_exceeded`) on UC-E refund/fulfilment-demand
shapes. Soft-signal candidate: a projection slot that names the
session's evidence dimensions (FAQ-search-evidence-count,
user-explicit-demand-detected); the LLM owns the choice. NO keyword /
regex / per-UC matrix on user content.

The brief's hypothetical UC-E-specific reason
(`refund_or_fulfillment_required`, line 16) is an
`escalation_reason` enum extension proposal that lives on the
phase-2 / `EscalationTrigger` enum surface — out of Sprint 21
scope; can be a separate downstream R-item if the
`R-cs176-semantic-planner-escalation-family-discrimination` work
shows the soft-signal route is insufficient.

### 3.6 R-cs192-secondary-ucs-duplicate-uc-b → APPROVED override

**Source brief:** `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md`
**Source session:** `570Q5000008rbcjIAA`
**Disposition class:** approved
**Override entry:** appended in the new Sprint 21 block. New
`classification.secondary_ucs: [UC-K, UC-D]` (UC-B duplicate removed).

**Evidence walk.** The CaseSpec at
`eval_interactive/case_specs/smoke/cs_interactive_192.yaml` lines
24–29 has `classification.primary_uc: UC-B` and
`classification.secondary_ucs: [UC-K, UC-D, UC-B]` — UC-B duplicated.
The brief identifies this on lines 17–18 (Ground-truth chain) and
lines 101 (Related observation #4) as a generator quirk: the L1 rule
extractor appended UC-B from a separate signal source without
deduplicating against `primary_uc`. The cs_192 transcript across
turns 0 and 1 (form description "I want to give away free items.
Can I do this on your site? And if so then how do I do it?" and
follow-up "OK. What items are allowed?") shows no UC-B-distinct-from-
primary signal anywhere.

**§1.7 self-check.** Zero-scoring-impact correction: the L2
`correct_uc` check compares the bot's stamped UC to the union of
primary and secondary; the dedupe leaves that union unchanged. The
bot's separate UC-B template-escalate failure on this resolvable
giveaway question is captured by the Sprint 20 cs192 case family and
is NOT affected by this override. This is the §1.7-cleanest possible
override — generator quirk corrected; no bot pass/fail change.

**Artefact.** `case_spec_overrides.yaml` entry around line 471 (the
Sprint 21 block's last entry). See §4 for the deltas table.

### 3.7 R-generator-get-customer-context-policy-mismatch → REJECTED as non-eval_spec

**Source briefs:** cs_001, cs_011, cs_259 (UC-C, UC-D, UC-F).
**Source sessions:** `570Q5000008kr6LIAQ`, `570Q5000008NWIjIAO`,
`570Q5000008OqaLIAS`.
**Disposition class:** rejected
**Reason:** the layer is `product_policy` (widen phase 2 §2.10 line
358), not `eval_spec` (drop the tool from CaseSpec
`expected_tool_sequence`).

**Evidence walk.** The brief at
`docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md`
line 70–75 (Related observation, broadened from cs_001) explicitly
names TWO remediation paths:

> *multiple CaseSpecs likely need either an L3 override (drop
> `get_customer_context` from sequence) or a phase 2 policy widening
> (add UC-C / UC-D to the allowed list). The remediation path
> depends on the product / privacy intent behind the original phase 2
> restriction.*

cs_259 (UC-F) reinforces this on line 100 of its brief: "Three
confirmed instances across three UCs. The 'systematic generator-vs-
policy mismatch' hypothesis from cs_011 is solid now."

The human Sprint 21 decision: **Path B (phase 2 widening for UC-C +
UC-D + UC-F)**. This routes the disposition to `product_policy`
under §3.1: *"product_policy — whether the underlying ask is a
product / policy decision the bot cannot make alone."* The bot's
actual behaviour on each of cs_001 / cs_011 / cs_259 (skipping
`get_customer_context` on a UC the runtime currently forbids it for)
was correct under the delivered runtime; the CaseSpec generator's
inclusion of the tool was the wrong artefact under the current phase
2 policy. The correct remediation widens phase 2 to align with the
human-reviewed expectation.

**§3.2 walk for layer reclassification.**

- Q1 (infra): no infra failure on the systematic shape.
- Q2 (java_guard / Tier-0): no Tier-0 invariant is being protected.
- Q3 (prompt_projection): the bot's lack of `get_customer_context`
  call is a runtime tool-policy enforcement, not a projection issue.
  The LLM does not currently have the tool callable on UC-C / UC-D /
  UC-F.
- Q4 (skill_state): n/a.
- Q5 (semantic_planner): n/a — the LLM did not have the choice.
- Q6 (eval_spec): the CaseSpec did include `get_customer_context` in
  `expected_tool_sequence`. Path A (drop from CaseSpec) WOULD be an
  eval_spec disposition. Path B (widen phase 2) is the human's
  decision — see Q7.
- Q7 (product_policy): **the underlying ask — should
  `get_customer_context` be allowed on account/messaging/payment UCs?
  — is a product / privacy decision.** First-match-wins for the
  decided path → `product_policy`.

**§1.7 self-check (rejection rationale).** The bot was correct under
current phase 2; the CaseSpec was incorrect under current phase 2.
Path A would have been a §1.7-clean approved override (align CaseSpec
with delivered runtime). Path B is also §1.7-clean — it aligns phase
2 (the source of truth for tool policy) with the human-reviewed
expectation. The §1.7 forbidden direction (widen eval to accept a
genuine bot mistake) is NOT taken in either path; the human chose
Path B because the underlying product question favoured policy
widening over CaseSpec narrowing.

**Disposition artefact.** No YAML write for cs_001 / cs_011 / cs_259
on this dimension (the cs_001 entry added in §3.1 is unrelated — it
fixes `escalation_trigger`, not the tool sequence). cs_011's
existing Sprint 4 §E1 entry is untouched (see §1 doc status warning
#2). New R-item proposed:
**`R-phase2-uc-cdf-customer-context-policy-widen`** — phase 2 §2.10
line 358 widening to add UC-C, UC-D, UC-F to the
`get_customer_context` allowlist. The remediation lands in a future
sprint that has `docs/foundational/**` in scope.

## 4. `case_spec_overrides.yaml` deltas

Schema v2 preserved (no schema change). Three entries added; one
legacy entry removed; one block-header comment updated. All paths
relative to the repo root.

| Action | source_session_id | block(s) | Location in file (post-edit) |
|---|---|---|---|
| ADDED | `570Q5000008kr6LIAQ` (cs_001) | `expected: {escalation_trigger, bot_handling_pattern}` | ~line 333 (new Sprint 21 block) |
| ADDED (supersedes Wave A2.1) | `570Q5000008U5C9IAK` (cs_095) | `classification: {primary_uc: UC-D, secondary_ucs: [UC-A, UC-K]}` | ~line 411 (new Sprint 21 block) |
| ADDED | `570Q5000008rbcjIAA` (cs_192) | `classification: {secondary_ucs: [UC-K, UC-D]}` | ~line 471 (new Sprint 21 block) |
| REMOVED (legacy) | `570Q5000008U5C9IAK` (cs_095) | legacy `migrated_from_legacy: true`, `classification: {primary_uc: UC-A, secondary_ucs: [UC-D, UC-K]}` | previously in Wave A2.1 legacy block |
| UPDATED (comment only) | n/a | Wave A2.1 header block | "9 remaining" → "8 remaining (plus cs_interactive_095 superseded by Sprint 21 below)" |

Verified post-edit by the actual loader at
`eval_interactive/eval_interactive/case_spec/extractor.py:_load_case_spec_overrides`:
17 applied entries (was 15), 0 pending, 0 duplicate
`source_session_id` collisions, all three new entries normalised
through `_normalise_classification_block` / `_normalise_expected_block`
without error. See §6 for the validation command.

## 5. Cascade rule observance

The sprint objective's hard fence on Sprint 20-authored case-family
content is the single non-negotiable governance bar:

> *Editing Sprint 20-authored case family content under any
> circumstance. Specifically, all paths under
> `eval_interactive/case_specs/case_families/**` (target / neighbor /
> negative subdirectories) and all paths under
> `eval_interactive/case_specs_shadow/case_families/**`.*

Files NOT touched by Sprint 21 (cascade-rule evidence):

- `eval_interactive/case_specs/case_families/cs001_uc_c_template_escalate/**`
- `eval_interactive/case_specs/case_families/cs011_uc_d_description_ignored/**`
- `eval_interactive/case_specs/case_families/cs015_uc_fp_mis_route/**`
- `eval_interactive/case_specs/case_families/cs038_uc_j_intake_redundancy/**`
- `eval_interactive/case_specs/case_families/cs040_uc_k_disengaged_jargon/**`
- `eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/**`
  ← **load-bearing**; family was authored assuming UC-D primary,
  cs_095 disposition CONFIRMS UC-D primary, family unchanged
- `eval_interactive/case_specs/case_families/cs176_uc_e_wrong_escalation_reason/**`
- `eval_interactive/case_specs/case_families/cs192_uc_b_giveaway/**`
- `eval_interactive/case_specs/case_families/cs259_uc_f_payment_question/**`
- `eval_interactive/case_specs/case_families/manual_probe_uc_a_resolve_must/**`
- `eval_interactive/case_specs_shadow/case_families/**` (all 10 family
  shadow directories)
- `eval_interactive/case_specs/case_families/_manifest.yaml`
- `eval_interactive/case_specs_shadow/_manifest.yaml`
- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`

The contingent future R-item
**`R-cs095-family-refresh-post-l3-reversal`** is recorded in §12 as a
no-op-this-sprint observation: if a future disposition reverses cs_095
UC primary back to UC-A, the Sprint 20 family's expected fields would
need a refresh. Sprint 21's UC-D confirmation matches the family's
authoring direction, so this R-item is OPENED-AND-DORMANT (does not
require action this sprint); the deliver agent can elect not to
include it in the action_bank §5.2 if the dormant-R-item convention
treats it as redundant. The handoff records the contingency
explicitly so a future re-review has the trail.

Also NOT touched by Sprint 21 (broader fence):

- `eval_interactive/case_specs/{smoke,anchor,promotion,exploration}/*`
- `eval_interactive/personas*.yaml`, `eval_interactive/data/**`
- `server/**`, `data/**`,
  `FAQ-knowledge_include_help_url.csv`
- `docs/sprints/sprint-001..020-*` (sprint archives)
- `docs/archive/**`, `docs/foundational/**`, `docs/current/**`
- `docs/runtime_freeze_and_risk_policy.md`, `AGENTS.md`, `CLAUDE.md`
- `docs/sprint_objective.md`, `docs/codex-findings.md` (deliver-agent
  surfaces; rolled forward at sprint close)

## 6. Files changed

Sprint 21 dev-agent commits touch three files:

- `eval_interactive/case_spec_overrides.yaml` — EDITED. Three new
  Sprint 21 entries inserted between the cs_029 entry and the Wave
  A2.1 legacy block (new block at lines ~298–490 of the post-edit
  file); the previous cs_095 Wave A2.1 legacy entry removed from the
  legacy block; the Wave A2.1 block header comment updated from
  "9 remaining" to "8 remaining (plus cs_interactive_095 superseded
  ...)".
- `docs/sprints/sprint-021-handoff.md` — NEW (this file).
- `docs/10-handoff.md` — EDITED. Current-phase paragraph rewritten to
  lead with Sprint 21; Sprint 20 demoted to "Preceding sprint";
  Sprint 19 / Sprint 18 / Sprint 17 detail paragraphs below the lead
  preserved.

No edit to any file under `server/`, `eval_interactive/eval_interactive/`,
`eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`,
`eval_interactive/scripts/`, `eval_interactive/tests/`, `data/`,
`scripts/`, `docs/current/`, `docs/foundational/`, `docs/sprints/sprint-001..020-*`,
`docs/diagnostics/`, `docs/proposals/`, or `docs/runbooks/`.

The working-tree artefacts at sprint start (`docs/sprint_objective.md`
Sprint 21 objective, `compact/sprint-021-dev-prompt.md`,
`compact/sprint-021-review-prompt.md`, `compact/sprint-deliver-orchestrator.md`)
are deliver-agent-owned and are NOT staged in the dev-agent commit.
The deliver agent rolls them forward at sprint close per the
Sprint 20 close precedent (see Sprint 20 fix re-review's
out_of_scope_review packaging convention).

**Validation command (sanity check).** Run from repo root:

```
/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/.venv/bin/python -c "
import sys
sys.path.insert(0, '/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive')
from eval_interactive.case_spec.extractor import _load_case_spec_overrides
reg = _load_case_spec_overrides('/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/case_spec_overrides.yaml')
print('applied:', len(reg.applied), 'pending:', len(reg.pending))
for sid in ['570Q5000008kr6LIAQ', '570Q5000008U5C9IAK', '570Q5000008rbcjIAA']:
    e = reg.applied[sid]
    print(sid, e.status, e.date, 'classification:', e.classification, 'expected keys:', list((e.expected or {}).keys()))
"
```

Expected output: `applied: 17 pending: 0`, three entries print with
the new classification / expected blocks. No schema validation
error.

## 7. Anti-hardcode self-walk (§4.1, 9 questions)

The Sprint 21 PR is semantic-touching on the eval_spec surface (three
approved overrides). §4.1 9-question walk applied to the SPRINT-LEVEL
deliverable (three approved entries + two rejected dispositions + two
deferred dispositions + four new R-item proposals).

1. **Keyword / regex / if-else / enum / per-UC matrix added for a
   semantic decision?** No. Each approved override is per-
   `source_session_id` ground truth; none is a runtime branch or
   prompt rule. The two rejected dispositions name layers (§3.5
   `semantic_planner`, §3.7 `product_policy`); neither remediation
   ships in Sprint 21.
2. **Tier-0 invariant justification?** N/A. No Tier-0 invariant added.
   The cs_040 deferral specifically routes the Tier-0-reading
   direction to the existing
   `R-escalation-reason-runtime-evidence-contract-review` candidate
   (out of Sprint 21 scope).
3. **Soft-signal projection alternative considered?** Yes for §3.5
   (cs_176 → `R-cs176-semantic-planner-escalation-family-discrimination`
   proposes a projection slot for evidence dimensions, LLM owns the
   choice; explicitly NO keyword / regex on user content). N/A for
   the eval_spec approved overrides (they describe per-case ground
   truth, not runtime decisions).
4. **Visible-eval case text encoded into runtime / prompt / judge?**
   No. No CaseSpec id, source_session_id, or user-message phrase is
   written into runtime / prompt / judge code. The override YAML
   itself cites case ids in `case_id_hint` (the schema's intended
   field for cross-reference) and source sessions in
   `source_session_id` (the lookup key); these are eval-surface
   artefacts, not runtime / prompt / judge.
5. **Semantic ownership moved from LLM to Java?** No. Constitution
   §1.3 LLM-owns scope unchanged. No runtime change shipped.
6. **Prompt grew an if-else block?** No prompt edit.
7. **Tool schema, capability / permission, PII / safety floor,
   grounding floor preserved?** Yes. No tool schema edit. No
   permission boundary change (the §3.7 disposition NAMES phase 2
   §2.10 line 358 but does NOT edit it — the widening is deferred
   to a future sprint). No PII surface change; override
   `rationale:` fields paraphrase user form descriptions but
   reference no PII beyond what is already in the source brief and
   the smoke CaseSpec. Grounding-floor diagnostics
   (`docs/current/faq_grounding_contract.md`) untouched.
8. **Generalization eval coverage (target / neighbor / negative /
   shadow)?** Inherited from Sprint 20. Each approved override
   corresponds to one Sprint 20-authored case family that already
   provides target + 2 neighbor + 2 negative + 2 shadow cases per
   `iteration_governance.md` §5.1. The Sprint 21 disposition does
   not edit any family file — see §9 for the coverage statement per
   approved override.
9. **Temporary change → sunset plan?** The three approved overrides
   are durable corrections (not temporary). The two rejected
   dispositions are durable layer reclassifications. The two
   deferred dispositions name an explicit dependency
   (`R-case-spec-overrides-schema-scoring-extension`) the human can
   sequence into a future sprint to unblock the originally-intended
   approved-with-tightening direction.

**Sprint 21 PR-level verdict (per §4.1):** **`approve`** — three
approved overrides are per-case eval_spec ground truth corrections;
two rejected dispositions reclassify to the correct layer with new
R-items naming the remediation; two deferred dispositions name the
schema-extension dependency. No semantic hardcode introduced; no
Tier-0 invariant added; no `human_review_required` flag fires beyond
what §3.3 routes back to the pre-existing Tier-0 candidate.

## 8. Layer-classification self-walk (§3) for rejected R-items

### 8.1 cs_176 (§3.5) reclassified eval_spec → `semantic_planner`

§3.2 first-match-wins walk landed at Q5 (semantic_planner) — see §3.5
above for the full Q1–Q5 trace. The bot had both
`user_requested` and `faq_miss_threshold_exceeded` available in the
escalation_trigger enum; the projection surfaced the user's explicit
demand unambiguously; the LLM chose the wrong family. New R-item:
`R-cs176-semantic-planner-escalation-family-discrimination`.

### 8.2 Systematic R-item (§3.7) reclassified eval_spec → `product_policy`

§3.2 first-match-wins walk landed at Q7 (product_policy) per the
human's Path B decision — see §3.7 above. The CaseSpec generator's
inclusion of `get_customer_context` for UC-C / UC-D / UC-F is correct
under the brief's human-reviewed expectation; phase 2 §2.10 line
358's current allowlist (UC-A / UC-FP / UC-K only) is the policy
artefact that needs widening. New R-item:
`R-phase2-uc-cdf-customer-context-policy-widen`.

### 8.3 cs_038 + cs_040 deferred (§3.2, §3.3): no layer reclassification

These are SCHEMA-blocked dispositions, not layer-mis-classified
dispositions. The underlying brief-level layer hypothesis remains
intact (§3.2 cs_038 → `semantic_planner` / `skill_state` + judge
calibration; §3.3 cs_040 → `semantic_planner` + Tier-0 candidate
`R-escalation-reason-runtime-evidence-contract-review`). The R-item
deferral is gated by `R-case-spec-overrides-schema-scoring-extension`
landing; once that lands, the L3 disposition can be revisited and
moved to approved.

§3.3 "Why no Java guard by default" check (§3 final paragraph of
`iteration_governance.md`): no Sprint 21 disposition names
`java_guard` as primary. cs_040's reading-as-Tier-0 direction
(§3.3) is explicitly routed back to the existing Tier-0 candidate
per §3.2 Q2 + sprint objective.

## 9. Generalization-coverage statement per approved override

§5.1's "Neighbor / negative no regression" + "Shadow no regression"
bars are satisfied by INSPECTION: Sprint 21 edits NO Sprint 20-
authored family file. Per-override coverage:

| approved override | family | target | neighbor | negative | shadow | consistency |
|---|---|---|---|---|---|---|
| cs_001 escalation_trigger flip | `cs001_uc_c_template_escalate` | ref | 2 | 2 | 2 | the family's `expected.escalation_trigger` is inherited from the smoke target ref; the override applies BEFORE family expansion, so the family's neighbor/negative cases that inherit `expected.*` from the target now see the corrected `faq_miss_threshold_exceeded` reason. No family file edit needed; consistency is structural. |
| cs_095 classification flip | `cs095_uc_classification_account_aware` | ref | 2 | 2 | 2 | the family was AUTHORED assuming UC-D primary (Sprint 20 §3.6); the override CONFIRMS UC-D primary, so the family neighbor/negative/shadow cases (which were authored to UC-D-primary expectations) are consistent without edit. Cascade rule satisfied. |
| cs_192 secondary_ucs dedupe | `cs192_uc_b_giveaway` | ref | 2 | 2 | 2 | zero-scoring impact (the union of primary + secondary is unchanged); family neighbor/negative/shadow inherit `classification.*` from the smoke target ref through the override; consistency structural. |

No regression on neighbors / negatives / shadows by inspection, since
the Sprint 21 deliverable changes no scoring outcomes for any
family-class case other than potentially the targets — and even
those changes are scoring-neutral for cs_095 (the override fixes the
upstream classification; the bot's L1 stall hard-fail on cs_095 is
independent), scoring-shift for cs_001 (corrects the
`expected.escalation_trigger` to match the bot's already-correct
emission), and scoring-neutral for cs_192 (dedupe leaves the
primary+secondary union unchanged).

## 10. Sprint-objective-met check

Walking `docs/sprint_objective.md` §"Success metrics" + §"Do not
implement".

### "Do not implement" (all ✓)

- No edit to Sprint 20-authored case-family content. ✓ §5 above.
- No widening of judge rubric to accept the bot's actual output
  when the bot was genuinely wrong. ✓ §1.7 self-check on every
  approved override (§3.1, §3.4, §3.6) and on every rejected /
  deferred disposition (§3.2, §3.3, §3.5, §3.7); cs_176 rejection is
  the explicit example of declining to widen.
- No edit to smoke / anchor / promotion / exploration CaseSpec. ✓
  The §1.7 forbidden surface is respected; all eval_spec corrections
  land in `case_spec_overrides.yaml`, not in inline CaseSpec edits.
- No edit to prompt / server / FAQ corpus / sprint archives /
  governance docs. ✓ §6 files-changed list.
- No re-run of smoke beyond the override-parse sanity check. ✓ The
  §6 validation command is a one-shot dry run via the actual loader
  on the override file; no smoke / eval / judge re-run.
- No new Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md`.
  ✓ cs_040's Tier-0 direction routed to the existing candidate per
  §3.3 + §3.2 Q2.
- No work on other R-items in `docs/action_bank.md` §5.2 not named
  in this objective. ✓
- No edit to `docs/sprint_objective.md`, `docs/codex-findings.md`,
  any sprint archive other than this new Sprint 21 handoff. ✓
- No architecture-health metric collection opened. ✓

### "Success metrics" (all ✓)

- **7 L3 dispositions delivered.** ✓ §3.1–§3.7 above. Classification
  summary: 3 approved (cs_001, cs_095, cs_192), 2 rejected (cs_176,
  systematic), 2 deferred (cs_038, cs_040).
- **Approved overrides land in `case_spec_overrides.yaml`.** ✓ Three
  new entries verified by the loader (§6 validation command). Each
  carries `status: approved`, `source:`, `reviewer: human semantic
  review (Sprint 21 L3 batch)`, `date: 2026-05-14`, `confidence:
  high`, populated `supporting_turn_numbers`, `rationale:` citing
  the source brief + the Constitution clause justifying the override.
- **Rejected R-items produce a new R-item proposal.** ✓ cs_176 →
  `R-cs176-semantic-planner-escalation-family-discrimination`;
  systematic → `R-phase2-uc-cdf-customer-context-policy-widen`. See
  §12.
- **Deferred R-items carry forward with a reason.** ✓ cs_038 +
  cs_040 → blocked on `R-case-spec-overrides-schema-scoring-extension`.
  See §12.
- **§1.7 self-check passes for every approved override.** ✓ §3.1,
  §3.4, §3.6 each contain a §1.7 self-check sub-section with quoted
  brief evidence.
- **Generalization-coverage statement per approved override.** ✓ §9.
- **Neighbor / negative no regression.** ✓ §5 + §9 — no Sprint 20-
  authored family content edited.
- **Safety floor unchanged.** ✓ No PII / safety / identity-verification
  surface edited. The override entries paraphrase user form
  descriptions already present in the source briefs and smoke
  CaseSpecs.
- **Grounding floor unchanged.** ✓ No edit to
  `docs/current/faq_grounding_contract.md`, FAQ corpus, or grounding
  diagnostics.
- **Wrong-containment / over-escalation rate unchanged or down.** ✓
  Trivially. Sprint 21 ships no agent-behaviour change; the eval-
  spec corrections do not affect bot decisions.
- **Architecture-health metrics not regressed.** ✓ §6 metrics remain
  `collection_status: not_started`.

## 11. Open questions for human

1. **cs_011 expected_tool_sequence carries an internal
   inconsistency until phase 2 widening lands.** The existing Sprint
   4 §E1 override (untouched this sprint) has
   `expected.bot_handling_pattern` naming `get_customer_context`,
   which phase 2 currently forbids for UC-D. With Sprint 21's
   systematic disposition routing the fix to phase 2 widening
   (`R-phase2-uc-cdf-customer-context-policy-widen`), the cs_011
   wording becomes consistent the moment phase 2 widens. **Open
   question:** is the deliver agent expected to edit the cs_011
   override entry's `bot_handling_pattern` wording as part of the
   phase-2-widening sprint at close, or should the next eval-
   governance sprint pick that as a small follow-on once the phase 2
   widening is in?
2. **cs_095 UC-D-primary classification depends on phase 2 widening
   for `get_customer_context` for full consistency.** Until
   `R-phase2-uc-cdf-customer-context-policy-widen` lands, a UC-D-
   primary cs_095 has the same wording-vs-tool-allowlist gap as
   cs_011 (the override classification mandates an account-aware
   bot, but phase 2 forbids the account-aware tool for UC-D).
   **Open question:** does this gap warrant any interim notation in
   the cs_095 family manifest (Sprint 20-authored, not edited this
   sprint per the cascade rule), or is the dependency adequately
   captured by the action_bank §5.2 row?
3. **`R-cs095-family-refresh-post-l3-reversal` opening posture.** Per
   §5, this R-item is contingent (fires only if a future disposition
   reverses cs_095 UC primary back to UC-A). Sprint 21's UC-D
   confirmation makes it dormant by construction. **Open question:**
   should the deliver agent record it in `docs/action_bank.md` §5.2
   as a dormant tracked-R-item (so the trail is durable), or leave
   it as a §5 handoff note only (avoiding action-bank noise)?
4. **`R-case-spec-overrides-schema-scoring-extension` scoping.** The
   new R-item shared by cs_038 + cs_040 covers two distinct potential
   extension paths: (a) extend the override schema to permit
   `scoring.outcome_checks` / `scoring.hard_checks` membership
   overrides per `source_session_id`, or (b) add new `Expected`
   fields (`intake_completion_turn_target`, `min_intake_fields_collected_for_escalation_reason`,
   etc.) that scorer logic consumes. **Open question:** should the
   deliver agent record this as one composite R-item, or split into
   two with the (a) vs (b) trade-off named explicitly?
5. **`R-cs176-semantic-planner-escalation-family-discrimination`
   soft-signal vs phase-2-enum-extension.** Per §3.5, the brief's
   hypothetical `refund_or_fulfillment_required` UC-E-specific
   escalation reason is one possible long-term path; the soft-signal
   approach (project evidence dimensions, LLM owns the choice) is
   the §3 layer recommendation. **Open question:** should the
   action_bank row recommend the soft-signal path as the primary
   approach with the enum-extension as a fallback, or list them as
   co-equal sub-paths the next eval-governance sprint chooses
   between?

## 12. Action-bank deltas + next recommended action

The dev agent does NOT edit `docs/action_bank.md` in this PR; the
deltas below are recommended for the deliver agent to apply at
sprint close (matching the Sprint 18 / 19 / 20 convention).

### 12.1 Updated rows in §5.2

The seven R-items processed by Sprint 21 are all resolved on this
sprint. Recommended row-level dispositions:

- `R-cs001-escalation-trigger-l3-review` (per-case L3) — change
  disposition from `pending — Sprint 21 candidate` (or equivalent
  Sprint 20-era state) to **`done — Sprint 21 approved override
  (case_spec_overrides.yaml entry for 570Q5000008kr6LIAQ); see Sprint
  21 handoff §3.1`**.
- `R-cs038-l3-review-intake-efficiency` (per-case L3) — change to
  **`deferred — Sprint 21 schema-blocked; depends on
  R-case-spec-overrides-schema-scoring-extension; see Sprint 21
  handoff §3.2`**.
- `R-cs040-l3-review-intake-completion-semantics` (per-case L3) —
  change to **`deferred — Sprint 21 schema-blocked + Tier-0
  candidate territory; depends on
  R-case-spec-overrides-schema-scoring-extension (eval-spec path) OR
  R-escalation-reason-runtime-evidence-contract-review (runtime
  path); see Sprint 21 handoff §3.3`**.
- `R-cs095-uc-classification-l3-rereview` (per-case L3 — highest
  impact) — change to **`done — Sprint 21 approved override
  (case_spec_overrides.yaml entry for 570Q5000008U5C9IAK; supersedes
  Wave A2.1 legacy UC-A pin); UC primary flipped UC-A → UC-D, secondary
  [UC-D, UC-K] → [UC-A, UC-K]; cascade rule satisfied IN-DIRECTION
  (Sprint 20 cs_095 family unchanged); see Sprint 21 handoff §3.4 +
  §5`**.
- `R-cs176-escalation-reason-l3-review` (per-case L3) — change to
  **`rejected — Sprint 21 reclassified to semantic_planner
  (CaseSpec user_requested is correct; bot chose wrong family);
  remediation via R-cs176-semantic-planner-escalation-family-discrimination;
  see Sprint 21 handoff §3.5`**.
- `R-cs192-secondary-ucs-duplicate-uc-b` (per-case L3, low impact) —
  change to **`done — Sprint 21 approved override
  (case_spec_overrides.yaml entry for 570Q5000008rbcjIAA); secondary_ucs
  deduped [UC-K, UC-D, UC-B] → [UC-K, UC-D]; see Sprint 21 handoff
  §3.6`**.
- `R-generator-get-customer-context-policy-mismatch` (systematic,
  spans cs_001 / cs_011 / cs_259) — change to **`rejected —
  Sprint 21 reclassified to product_policy (phase 2 §2.10 line 358
  widening); remediation via R-phase2-uc-cdf-customer-context-policy-widen;
  see Sprint 21 handoff §3.7`**.

### 12.2 New R-items proposed

Four new R-items surface from Sprint 21's dispositions:

- **`R-phase2-uc-cdf-customer-context-policy-widen`** (`product_policy`).
  Phase 2 §2.10 line 358 widening to add UC-C, UC-D, UC-F to the
  `get_customer_context` allowlist. Scope:
  `docs/foundational/phase2_domain_realization_spec.md` §2.10 line
  358; downstream runtime tool-policy update in `server/` if any
  Java-side guard mirrors the allowlist. Source: Sprint 21 §3.7
  systematic disposition. Confidence: high (n=3 across cs_001 /
  cs_011 / cs_259, human-reviewed). Recommended for the next
  product-policy / phase-2 fold-back sprint.
- **`R-cs176-semantic-planner-escalation-family-discrimination`**
  (`semantic_planner`). The LLM must discriminate user-intent
  escalation families (`user_requested`) from bot-limit escalation
  families (`faq_miss_threshold_exceeded`) on UC-E refund/fulfillment
  -demand shapes. Soft-signal candidate: a projection slot naming
  evidence dimensions (FAQ-search-evidence-count, user-explicit-demand-
  detected); LLM owns the choice; NO keyword / regex / per-UC matrix
  on user content. Source: Sprint 21 §3.5 cs_176 rejection. Scope:
  prompt / projection / runtime intentionally undecided — the next
  remediation sprint walks §3.2 fresh on the candidate fix.
- **`R-case-spec-overrides-schema-scoring-extension`** (`infra` /
  eval harness). Extend the override schema at
  `eval_interactive/eval_interactive/case_spec/extractor.py:_CASE_OVERRIDE_EXPECTED_FIELDS`
  + `extractor.py:_normalise_expected_block` to permit `scoring.*`
  overrides per `source_session_id` (e.g. moving a check between
  `outcome_checks` and `hard_checks`). Alternative or complementary
  path: add new `Expected` dataclass fields
  (`intake_completion_turn_target`, `min_intake_fields_collected`,
  etc.) that scorer logic consumes. Source: Sprint 21 §3.2 (cs_038
  deferral) + §3.3 (cs_040 deferral). Unblocks the two deferred
  Sprint 21 R-items. See §11 Q4 for scoping sub-question.
- **`R-cs095-family-refresh-post-l3-reversal`** (eval governance,
  contingent / DORMANT). Triggered only if a future disposition
  reverses cs_095 UC primary back to UC-A (or to any non-UC-D
  primary). The Sprint 20-authored cs_095 family at
  `eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/`
  and `eval_interactive/case_specs_shadow/case_families/cs095_uc_classification_account_aware/`
  was authored assuming UC-D primary; reversal would require the
  family's expected fields to refresh. Source: Sprint 21 §3.4 +
  Sprint 20 §11 Q2. Currently DORMANT (Sprint 21 UC-D confirmation
  matches the family's authoring direction). See §11 Q3 for
  recording-posture question.

### 12.3 Out-of-scope deferrals (Sprint 21 did NOT investigate)

All R-items in `docs/action_bank.md` §5.2 not named above
(`R-uc-b-customer-context-policy-review`,
`R-l3-judge-form-context-trust-rubric`,
`R-corpus-coverage-audit-per-uc`,
`R-faqMissCount-threshold-and-timing-review`,
`R-duplicated-greeting-projection-fix`,
`R-escalation-reason-runtime-evidence-contract-review`,
`R-l1-source-citation-quality-rubric`,
`R-g2-multi-turn-followup-case-family-design`,
`R-runtime-orchestrator-tool-call-deduplication`,
`R-smoke-regression-investigation`, the seven Sprint 19 surfaced
R-items, the three Sprint 20 surfaced R-items). All explicit per
`docs/sprint_objective.md` §"Do not implement".

G3+ remediation of any G1 brief failure shape is NOT performed in
Sprint 21. Sprint 21 produces L3 dispositions only.

### 12.4 Next recommended action

**Recommended next sprint: `R-phase2-uc-cdf-customer-context-policy-widen`**.
Three reasons:

1. **It unblocks cs_011's existing override wording inconsistency**
   (§11 Q1) and cs_095's UC-D-primary contract gap (§11 Q2)
   simultaneously. Both are small but real durability concerns on
   the eval-spec surface that the next sprint cleanly closes.
2. **Phase 2 widening is small, well-scoped, and has clear
   human-reviewed precedent** (n=3 across cs_001 / cs_011 / cs_259,
   plus the Sprint 21 human decision documented in §3.7). The
   product-policy review surface is small (one line in phase 2 §2.10
   + the bot's runtime tool-policy mirror if any).
3. **No conflict with `R-case-spec-overrides-schema-scoring-extension`**
   (the second-largest Sprint-21-surfaced R-item). The two are
   disjoint surfaces and could run in parallel; the phase 2 widening
   is the shorter / lower-risk pick.

**Parallel candidate: `R-case-spec-overrides-schema-scoring-extension`**
— unblocks cs_038 + cs_040 deferred dispositions. Larger scope (Python
schema extension + scorer integration). Suitable for an
eval-harness-focused sprint after the phase 2 widening.

**Conditional follow-on: `R-cs176-semantic-planner-escalation-family-discrimination`**
— a `semantic_planner` remediation that the next G3+ remediation
sprint can pick up once `R-slow-llm-placeholder-coalesce` and
`R-prompt-phase-plan-directive-followship` (the Sprint 19 backlog
items currently blocking cs_011-shape remediation) are in motion.
