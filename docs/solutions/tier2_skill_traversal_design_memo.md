---
title: Tier-2 Skill-traversal design memo — #9 design decision for S-Cleanup-3
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (design memo; feeds the human a/b/c decision for S-Cleanup-3)
last_reviewed: 2026-05-23
review_cadence: ad hoc (single-use planning artefact)
supersedes: []
superseded_by: null
notes: >
  Deliver-agent authored 2026-05-23 to inform the human's a/b/c decision
  on the M4-Eval-Cleanup audit item #9 (Tier-2 full-Skill traversal vs
  runtime-selected Skill) BEFORE the S-Cleanup-3 contract is drafted.
  Per `iteration_governance.md` §5.5/§5.6 + the M3-Eval four-tier pyramid.
  This memo includes an OFFLINE reproduction of the misflip the audit
  flagged; the finding changes the decision from "document a design
  choice" to "fix a Tier-2 gate bug".
---

# Tier-2 Skill-traversal design memo — #9 design decision for S-Cleanup-3

## 0. TL;DR

The audit's #9 ("Tier-2 traverses all Skills rather than the runtime-
selected Skill — may misflip `case_passed`; needs product decision") is
**confirmed concrete, not theoretical**. An offline reproduction shows a
correct UC-A FAQ-resolve session (no escalation) is flipped to
`case_passed = False` by a mandatory Tier-2 step belonging to a Skill the
session never entered. Option (a) "keep + document" is therefore refuted
by evidence. The recommended fix is **option (b): scope Tier-2 evaluation
to the critical-steps the runtime actually presented** (derivable from
`per_turn_trace[].phase_plan.critical_steps[].id`, which the trace already
carries) — an **eval-side-only** change that respects the milestone's
"no `server/` touch" hard fence.

## 1. Purpose

This memo feeds the human's a/b/c decision on audit item #9 BEFORE the
S-Cleanup-3 contract is drafted (per the 2026-05-23 deliver-agent + human
direction "先出 Tier-2 design memo"). It is a single-use planning
artefact, not a governance doc.

## 2. The current mechanism

Tier-2 `skill_procedure_followship` is computed in
`eval_interactive/eval_interactive/batch/executor.py:_compute_tier2_result`
(lines 365-392). The relevant logic:

```python
ext = self._get_skill_extractor()                 # loads ALL 6 Skill YAMLs
all_results = []
for skill_name in ext.skills_by_name:              # iterates EVERY loaded Skill
    all_results.extend(ext.extract(per_turn_trace, skill_name, active_use_case))
return tier2_results_to_gate(all_results)
```

`SkillProcedureExtractor.extract(trace, skill, active_use_case)`
(`scoring/skill_procedure_check.py:639-699`) evaluates each of the
Skill's `critical_steps`, returning `N/A` for any step whose
`mandatory_for` UC list does NOT contain `active_use_case`, and
`PASS`/`FAIL` otherwise. `tier2_results_to_gate` (lines 730-771) flips
`case_passed` to False iff **any mandatory step FAILs**.

**The gap**: the only filter is `active_use_case ∈ step.mandatory_for`.
There is NO filter for *which Skill the runtime actually traversed*. The
docstring acknowledges this is deliberate ("iterating every loaded Skill
is safe ... avoids hard-coding a per-(phase, UC)-to-Skill mapping on the
eval side") — but the "safe" claim rests on the assumption that
`mandatory_for` membership is a sufficient proxy for "this step applies
to this session". §4 shows that assumption is false.

## 3. The confirmed misflip (offline reproduction)

The `escalate` Skill's mandatory step is the smoking gun
(`server/src/main/resources/skills/escalate.yaml:31-54`):

```yaml
- id: escalate-via-request-handover
  trace_check: "accumulated_tool_results.request_handover"
  mandatory_for: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP, UC-G, UC-H, UC-I, UC-J, UC-K]   # ALL 12 UCs
  severity: mandatory
```

This step is mandatory for **every** UC, and its `trace_check` requires
`request_handover` to have been dispatched. Combined with full-Skill
traversal, this means: **any session in any UC that resolves WITHOUT
escalating fails a mandatory Tier-2 step** — even though not escalating
was the correct behaviour.

Offline reproduction (pure-Python, no backend; run 2026-05-23 against
HEAD `b989833`): a synthetic UC-A FAQ-resolve session with
`search_knowledge` → `resolve_article` → `record_outcome` and NO
`request_handover`:

```
Tier-2 gate passed: False | severity: critical
failed_step_ids: ['escalate-via-request-handover']

Per mandatory step evaluated for UC-A (non-N/A):
  [PASS] record-outcome-on-confirmed-resolve  (mandatory)   ← confirm Skill
  [FAIL] faq-uc-search-before-commit          (advisory)    ← discover Skill
  [FAIL] escalate-via-request-handover        (mandatory)   ← escalate Skill ✗ FLIPS
  [FAIL] record-outcome-after-handover         (advisory)   ← escalate Skill
  [PASS] search-knowledge-before-faq-answer   (mandatory)   ← resolve_faq Skill
  [PASS] resolve-article-after-search-hit     (advisory)    ← resolve_faq Skill
  [PASS] record-outcome-on-grounded-answer    (advisory)    ← resolve_faq Skill
  [PASS] terminal-records-outcome             (mandatory)   ← terminal Skill
```

The session correctly resolved via FAQ and never entered the ESCALATE
phase, yet the escalate Skill's mandatory step was evaluated (because
UC-A ∈ its `mandatory_for`) and FAILed (no `request_handover`), flipping
`case_passed` to False. **This is a wrong flip.**

The symmetric case also exists: a UC-A *escalation* session that does not
resolve via FAQ would FAIL `search-knowledge-before-faq-answer` (mandatory
for UC-A) — another wrong flip in the opposite direction. The two
mandatory steps are mutually exclusive for a single session, so under
full-Skill traversal **at least one mandatory step fails on essentially
every UC-A/UC-B session**.

## 4. Why `mandatory_for` is not a sufficient proxy

Each `critical_step.mandatory_for` was authored (S-Eval-3) to mean "this
step is mandatory **within its Skill's phase/path** for these UCs". The
implicit qualifier "within its Skill" is the runtime contract: the
runtime's `SkillRegistry.select` picks ONE Skill per (phase, UC), and a
session traverses a *sequence* of phases (DISCOVER → CONFIRM → RESOLVE →
maybe ESCALATE → TERMINAL). A given session enters only the Skills its
path actually requires. `mandatory_for` is the UC scope *given that the
Skill's phase was entered* — it was never meant to be evaluated against a
session that never entered that phase.

Full-Skill traversal drops the "given that the Skill's phase was entered"
qualifier, which is the entire bug.

## 5. Scope of impact

- **Programmatic-authority suites** (`anchor` = 159 cases; also
  `promotion` / `exploration` / `smoke`): `case_passed` is gated by Tier-2
  per the M3-Eval Option A executor wiring (S-Eval-3 / S-Eval-5). These
  suites are exposed to the misflip. Any non-escalating case (the
  majority of resolve-path cases) is at risk of a spurious mandatory
  Tier-2 FAIL.
- **Human-judgment suites** (`bad_cases` / `anchor_outcome`): as of
  S-Cleanup-2, these carry `case_passed_authority = "human_review"`; the
  programmatic `case_passed` is informational only (§5.6 gate is human
  review of `closure_criterion` against the trace). The misflip is
  **masked** for these suites — the human reads the trace regardless of
  the programmatic flip.
- This is why the bug has not blocked recent closes: the M3-Eval +
  M4-Eval-Cleanup closes gate on the human-review bad-case suite, and
  smoke `composite_score` is observation-only (§5.5). The latent misflip
  sits on the programmatic anchor suite, which has not been the close
  gate. **Recommend an empirical anchor-suite confirmation** as the first
  step of S-Cleanup-3 to quantify the pre-fix misflip rate (how many of
  the 159 anchor cases flip on the escalate-vs-resolve mandatory pair).

## 6. The fix enabler: the trace already carries runtime-presented steps

`per_turn_trace[].phase_plan.critical_steps` is populated by
`ContextProjectionBuilder` with the critical_steps of the Skill the
runtime selected for that turn/phase. Verified against
`results/20260523-024518/results.json`: each turn's
`phase_plan.critical_steps[]` carries `{id, desc}` (the runtime-presented
steps; `trace_check`/`mandatory_for`/`severity` stay in the Skill YAML and
are looked up by `id`). This means the eval side can determine **exactly
which critical-step ids the runtime presented across the session** —
i.e., which Skills/phases the session actually traversed — without
duplicating `SkillRegistry.select` and without any `server/` change.

## 7. Options

### (a) Keep full-Skill traversal + document the rationale — REFUTED

Status: **rejected by §3 evidence.** This is not a benign design choice;
it is a systematic misflip on the programmatic anchor suite. Documenting
it as intentional would enshrine a bug. (If the human nonetheless prefers
(a) for scope reasons — e.g., defer the fix to a later milestone and only
document the known-issue now — that is a legitimate *deferral*, but it
should be framed as "known Tier-2 misflip, fix deferred", not "intended
design".)

### (b) Phase-plan-scoped evaluation — RECOMMENDED

Restrict Tier-2 evaluation to the critical-step ids that appeared in the
session's `per_turn_trace[].phase_plan.critical_steps`. Concretely, in
`_compute_tier2_result`: collect the set of presented step ids from the
trace; when iterating loaded Skills' steps, evaluate a step only if its
`id` is in that set (else `N/A`). For the §3 UC-A FAQ-resolve session, the
ESCALATE phase was never entered, so `escalate-via-request-handover` would
not appear in any turn's `phase_plan.critical_steps` → not evaluated → no
spurious flip.

- **Feasibility**: eval-side only (`executor.py` + possibly a small helper
  in `skill_procedure_check.py`); the trace already carries the data
  (§6). **Respects the milestone §6 hard fence "no `server/` touch".**
- **Correctness**: mirrors production exactly — Tier-2 evaluates the steps
  the runtime actually presented, which is the single-source-of-truth
  `critical_steps[].desc` the M3-Eval pyramid already routes through both
  the runtime LLM and the eval side.
- **§1.7 check**: NOT a semantic hardcode. The scoping key is the set of
  step ids the runtime emitted into the trace — observable state, not a
  keyword/regex/per-UC matrix. No semantic decision moves LLM → Java.
- **Risk**: low. If a turn's `phase_plan.critical_steps` is empty/absent
  (older traces, error turns), the fix must default safely (evaluate
  nothing for that turn rather than fall back to all-Skills). Needs a
  defensive default + unit tests (target/neighbor/negative: a resolve
  session, an escalate session, a multi-phase session that legitimately
  enters both).

### (c) Hybrid phase-evidence gate

Keep full-Skill traversal but add a guard that only evaluates a Skill's
mandatory step if the trace shows the session entered that Skill's phase
(e.g., via `phase_plan.phase` markers or the Skill's characteristic tool
calls). This is strictly more complex than (b) and arrives at the same
place; (b) subsumes it by using the step-id set directly. Not recommended
unless (b) surfaces an edge case where phase-entry evidence is needed
beyond the step-id set.

## 8. Recommendation

**Option (b).** It fixes the confirmed misflip, mirrors production, stays
eval-side (respects the §6 hard fence), and is not a §1.7 hardcode. It
elevates S-Cleanup-3 from "governance documentation" to "Tier-2 gate bug
fix + `handover_completeness` demotion" — still within the M4-Eval-Cleanup
cleanup theme, layer `judge_calibration` (Tier-2 wiring) + `eval_spec`
(#4 handover_completeness). No Tier-0 invariant; no `server/` change.

Suggested S-Cleanup-3 shape if the human picks (b):

1. **Empirical confirmation first** — run the anchor suite (or a
   representative subset) at `parallel=1`, record how many cases the
   current full-Skill traversal misflips on the escalate-vs-resolve
   mandatory pair. This quantifies the bug + becomes the before/after
   evidence for the fix.
2. **#9 fix** — phase-plan-scoped Tier-2 evaluation in `_compute_tier2_result`
   (+ helper + defensive empty-trace default + target/neighbor/negative
   unit tests).
3. **#4 handover_completeness / case_id_present demotion** —
   `composite.py:81,84` `_conditional_mandatory_l2` removal from the
   escalate path (independent of #9; can land in the same sub-sprint).
4. **Codex per-sub-sprint review** — likely §4.3 trigger #2 (Tier-2 wiring
   change touches a semantic-scoring surface); deliver-agent + human decide
   at S-Cleanup-3 planning whether to dispatch Codex per-sub-sprint or defer
   to milestone close. Given this is now a bug fix on the gate that decides
   `case_passed`, a per-sub-sprint Codex review is advisable.

## 9. Open questions for the human

1. **Confirm option (b)** (recommended) vs (c) hybrid vs (a)-as-deferral
   (document the known misflip, defer the fix to a later milestone).
2. **Empirical anchor-suite confirmation** — run it as S-Cleanup-3 step 1
   (quantify the pre-fix misflip), or skip and fix directly on the
   offline-confirmed logic?
3. **Codex cadence for S-Cleanup-3** — per-sub-sprint review (advisable
   given it's now a `case_passed`-gate fix) vs defer to milestone close?
4. **Scope of #4 + #9 in one sub-sprint** — land both in S-Cleanup-3, or
   split (#9 fix in S-Cleanup-3, #4 demotion in a S-Cleanup-3b)? Both are
   small; bundling is reasonable, but #9 is now the higher-risk item.
