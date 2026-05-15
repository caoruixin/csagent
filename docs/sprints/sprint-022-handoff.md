# Sprint 22 Handoff — Phase 2 line 358 reconciliation + R-item closure

Date: 2026-05-14
Branch: `design-v1-without-human-review`
Sprint class: docs-only governance / scope correction.
Per `docs/current/iteration_governance.md` §7 the Layer-classification +
anti-hardcode stanza is **EXEMPT** under the docs-only / config-governance
exemption (precedent: Sprint 15, Sprint 16, Sprint 17, Sprint 18). No
stanza is required; none is written.

## 1. Outcome

Sprint 22 closed on 2026-05-14 as a narrow docs-only scope-correction
sprint. The premise carried forward from Sprint 21 — that phase 2
forbids `get_customer_context` for UC-C / UC-D / UC-F — was based on
a misread of UC-H-local prose inside the UC-H-01 YAML block (phase 2
line 358) as the cross-UC rule. The normative cross-UC matrix at
phase 2 §2.10.1 line 1098 already permits the tool for UC-C, UC-D,
and UC-F (corroborated by `docs/customer_service_tool_spec_v0_2.yaml`
line 260 and `docs/current/customer_service_tool_spec_v0_3.md` line
60). Sprint 22 reconciled the misleading prose, closed
`R-generator-get-customer-context-policy-mismatch` and the
routed-to `R-phase2-uc-cdf-customer-context-policy-widen` as
premise-invalidated, annotated the three affected Failure Briefs
with notes-style correction blocks (no rewrite), and opened a single
follow-on R-item (`R-uc-cdf-get-customer-context-bot-actual-usage`,
proposed / deferred) capturing the residual behavioural question
(does the bot actually call the tool when account-state matters?).
No runtime change. No policy widening. No CaseSpec / override /
judge / prompt / FAQ corpus / case-family edits. No Tier-0
candidacy.

## 2. Scope delivered

Seven directives executed in order:

1. **Phase 2 line 358 prose reconciliation.** Rewrote the UC-H-01
   YAML block prose annotation to stop enumerating allowed UCs and
   instead defer the cross-UC question to §2.10.1. New shape:
   `get_customer_context (UC-H 不可用 — 见 §2.10.1 cross-UC allowlist；
   UC-H 仅靠 user_message 提问收集标识符)`. Added minimal YAML front
   matter to `docs/foundational/phase2_domain_realization_spec.md`
   (the doc had no front matter; per `docs/current/doc_governance.md`
   "Front matter is added incrementally", an existing-doc marking
   on the directive's reconciliation path is in scope). Front matter
   carries `last_reviewed: 2026-05-14` and a `notes:` line recording
   the Sprint 22 reconciliation.
2. **`R-generator-get-customer-context-policy-mismatch` correction
   addendum.** Appended a `**Sprint 22 correction (2026-05-14):**`
   addendum to `docs/action_bank.md` line 396 explaining the
   premise misread, citing §2.10.1 line 1098 + the two corroborating
   policy surfaces, and routing the residual behavioural question
   to `R-uc-cdf-get-customer-context-bot-actual-usage`. Preserved
   the existing Sprint 21 `status: done` text.
3. **`R-phase2-uc-cdf-customer-context-policy-widen` closure as
   premise-invalidated.** Prefixed `docs/action_bank.md` line 559's
   row with `**status: done — closed as premise-invalidated by
   Sprint 22 (2026-05-14).**` and a one-paragraph explanation
   citing §2.10.1 line 1098 + the two corroborating policy
   surfaces. Preserved the original Sprint 21 description for
   history.
4. **§6 Closed action index row.** Added one Sprint 22 row to the
   closed-action index after the Sprint 21 row. Per the §5.2
   convention demonstrated by Sprint 21 (line 396's row keeps
   `status: done` in-table rather than moving to §6), line 559's
   row also stays in §5.2 with the closure-prefix marker.
5. **Notes-style correction blocks on three Failure Briefs.**
   Appended `> **Correction (Sprint 22, 2026-05-14):**` blocks under
   the affected anchor in each of `cs001-uc-c-template-escalate-on-faq-miss.md`
   (line 67 bullet 2), `cs011-uc-d-detailed-description-ignored-on-faq-miss.md`
   (line 70 paragraph), and `cs259-uc-f-sprint7-i0-violation-on-payment-question.md`
   (line 100 bullet 1). Original brief content is untouched; the
   `doc_governance.md` "Stale references" notes-style amendment
   guidance is followed verbatim (no rewrite, no retraction, no
   re-litigation of the brief authors' Layer hypotheses).
6. **New follow-on R-item.** Opened
   `R-uc-cdf-get-customer-context-bot-actual-usage` in a new
   "Sprint 22 surfaced behavioural R-items" sub-section of
   `docs/action_bank.md` §5.2. Layer hint:
   `prompt_projection` or `semantic_planner` (NOT `product_policy`).
   Source: cs_001 (UC-C) + cs_011 (UC-D) + cs_259 (UC-F).
   Disposition: `proposed (Sprint 22 close 2026-05-14); deferred
   for future investigation sprint`. Also added a Sprint 22 note
   to the "Sprint 21 open questions" sub-section rescinding the
   Sprint 21 §12.4 recommendation that the (now-closed) widening
   R-item be the next sprint.
7. **Handoff doc + 10-handoff refresh.** This file is the new
   Sprint 22 handoff. `docs/10-handoff.md` §1 lead refreshed to
   Sprint 22's close narrative; Sprint 21 content preserved below.

## 3. Files touched

| file | lines changed | nature |
|------|---------------|--------|
| `docs/foundational/phase2_domain_realization_spec.md` | added 18-line YAML front matter at top (lines 1–18); rewrote prose annotation on what was previously line 358 (now line 379) inside the UC-H-01 YAML block | reconciliation: prose defers to §2.10.1 cross-UC matrix; front-matter added per `doc_governance.md` schema |
| `docs/action_bank.md` | line 396 row (now expanded): Sprint 22 correction addendum appended; line 559 row (now expanded): premise-invalidated closure marker prefixed; new §5.2 sub-section "Sprint 22 surfaced behavioural R-items" with the new R-item row; Sprint 21 open-questions note appended; §6 index gains a Sprint 22 row | three R-item ops + §6 index update |
| `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md` | inside bullet 2 of the "Related observation" section, appended one `> **Correction (Sprint 22, 2026-05-14):**` block under the existing bullet text | notes-style amendment; no rewrite |
| `docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md` | inside the "Related observation — systematic `get_customer_context` ↔ phase 2 mismatch" section, appended one `> **Correction (Sprint 22, 2026-05-14):**` block between the section's body and the final closing paragraph | notes-style amendment; no rewrite |
| `docs/diagnostics/failure-briefs/cs259-uc-f-sprint7-i0-violation-on-payment-question.md` | inside bullet 1 of the "Related observation — no new R-items" section, appended one `> **Correction (Sprint 22, 2026-05-14):**` block under the existing bullet text | notes-style amendment; no rewrite |
| `docs/sprints/sprint-022-handoff.md` | NEW | this file |
| `docs/10-handoff.md` | §1 lead refresh to Sprint 22 close; Sprint 21 content preserved as "Preceding sprint" | docs-only |

No file outside this list was edited.

## 4. Tests / eval

Not applicable. Sprint 22 is docs-only. No runtime change, no test
change, no eval run. `docs/current_eval_baseline.md` was not
consulted and not edited.

## 5. Tier-0 invariants

None introduced. None touched. The Sprint 22 reconciliation operates
purely on docs governance surface: a UC-H-local prose annotation
deferred to the cross-UC matrix that has been the source of truth
all along. No Tier-0 candidate is opened.

## 6. Anti-hardcode self-walk

Trivially clean. Sprint 22 touches no semantic surface (no prompt,
no runtime semantic decision, no eval spec, no judge calibration,
no override / case-family / FAQ content). Per the per-PR §4.1
prompt's scope-exemption clause ("pure infra, docs-only,
config-governance, and characterization-test PRs are not subject
to this review"), the appropriate verdict is `approve` with the
exemption named. Spot-check evidence on the three load-bearing
questions:

- **Q1 (does the PR add a keyword / regex / if-else / enum / per-UC
  matrix for a semantic decision?)** — No. The phase 2 line-358
  edit removes a UC enumeration from a UC-H-local prose annotation
  and defers to §2.10.1; this is a *narrowing* of the local
  annotation's scope, not the addition of any branch / rule /
  matrix. The action_bank closures, brief correction notes, and
  new R-item are docs governance, not runtime / prompt / eval.
- **Q5 (does the change move semantic ownership from the LLM to
  Java?)** — No. No runtime code is touched. The new R-item
  explicitly identifies the residual behavioural question as
  `prompt_projection` or `semantic_planner` (LLM-owned per §1.3),
  and explicitly excludes `product_policy` from its layer hint.
- **Q7 (does the change preserve tool schema, capability /
  permission boundary, PII / safety floor, and grounding floor?)**
  — Yes. The tool schema and the cross-UC allowlist are untouched;
  Sprint 22 reconciles the *docs* to the policy that already ships.
  PII / safety / grounding surfaces are not in scope.

## 7. Architecture-health metrics

No change. The four metrics in
`docs/current/iteration_governance.md` §6 are defined-but-not-collected
as of Sprint 17 (collection status `not_started`). Sprint 22
introduces no semantic hardcode (`new_semantic_hardcode_count = 0`),
no soft-signal conversion (`soft_signal_conversion_count = 0`), no
change to the planner-ownership ratio (no runtime decision moved),
and no shadow-disagreement signal (no eval was run).

## 8. R-items closed / opened

**Closed (2):**

- `R-generator-get-customer-context-policy-mismatch`
  (`docs/action_bank.md` §5.2 "Systematic"). Originally closed by
  Sprint 21 §3.7 with reclassification to `product_policy` and
  routing to the widening R-item. Sprint 22 amends the closure
  rationale: the policy mismatch never existed. The Sprint 21
  rejection (as non-eval_spec) stands; the Sprint 21 routing
  decision is superseded.
- `R-phase2-uc-cdf-customer-context-policy-widen`
  (`docs/action_bank.md` §5.2 "Sprint 21 routed-to R-items").
  Closed as premise-invalidated. The widening was unnecessary; the
  policy already permits the tool for UC-C / UC-D / UC-F per phase
  2 §2.10.1 line 1098.

**Opened (1):**

- `R-uc-cdf-get-customer-context-bot-actual-usage`
  (`docs/action_bank.md` §5.2 "Sprint 22 surfaced behavioural
  R-items"). Layer hint `prompt_projection` or `semantic_planner`.
  Disposition: `proposed (Sprint 22 close 2026-05-14); deferred
  for future investigation sprint`. The substantive behavioural
  question the closed R-items had been carrying: on FAQ-miss
  shapes where account-state would plausibly matter in UC-C /
  UC-D / UC-F, the bot did not call `get_customer_context`
  despite the policy allowing it. Whether the per-turn projection
  surfaces the right signal or whether the semantic planner
  systematically under-calls the tool is the open question.

## 9. Open questions surfaced

1. **Bot actual usage of `get_customer_context` in UC-C / UC-D /
   UC-F (`R-uc-cdf-get-customer-context-bot-actual-usage`).**
   Three briefs (cs_001 / cs_011 / cs_259) document non-calling
   on FAQ-miss shapes. Investigation sprint should walk
   `docs/current/iteration_governance.md` §3.2 Q3 vs Q5 to
   discriminate whether projection or planner is the right
   layer. No remediation in Sprint 22.
2. **Foundational-doc front-matter rollout.** Phase 2 received
   minimal front matter as a side-effect of directive 1; the
   other foundational docs (`phase0`, `phase1`, `phase3`,
   `phase5`, `customer_service_agent_tech_spec.md`,
   `runtime_freeze_and_risk_policy.md`, `customer_service_agent_eval_spec.md`,
   `current_eval_baseline.md`, `release_gate.md`,
   `07-engineering-constraints.md`, `10-handoff.md`) still lack
   front matter. `doc_governance.md` calls this an incremental
   rollout; no action required in Sprint 22 but a future docs
   fold-back sprint should consider it.
3. **Sprint 21 §11 Q1 + Q2 re-routing.** Sprint 21's recommended
   next sprint (§12.4) was the now-closed widening R-item.
   Sprint 21 §11 Q1 (cs_011 override wording inconsistency) and
   Q2 (cs_095 UC-D-primary contract gap) are still open. The
   `R-case-spec-overrides-schema-scoring-extension` R-item is one
   candidate path; the recommended-next-sprint discussion below
   covers options.

## 10. Recommended next sprint

This is the dev agent's recommendation; the human picks the actual
next scope.

Three plausible candidates, in rough order of urgency:

- **A. Resume Sprint 19 Track B Handover Orchestrator work.** The
  Sprint 16 `docs/proposals/handover_orchestrator_design.md` design
  freeze still has no implementation. Sprint 19 §4.2 identified
  the write-side surface as load-bearing for the orchestrator-tool-call
  de-dup story; Sprint 20 Track B delivered the read-side `already_called`
  soft signal. Picking up the write-side is a natural Sprint 19
  carry-over and is one of the explicit
  `docs/release_gate.md` §1.1 blockers.
- **B. `R-case-spec-overrides-schema-scoring-extension`.** This
  unblocks the two Sprint 21 deferred dispositions (cs_038, cs_040)
  and is named in Sprint 21 §11 Q4 as a sequencing question. Scope
  is well-defined: extend the override schema at
  `eval_interactive/eval_interactive/case_spec/extractor.py:_CASE_OVERRIDE_EXPECTED_FIELDS`
  + `_normalise_expected_block` to permit `scoring.*` overrides per
  `source_session_id`, or add new `Expected` dataclass fields. This
  is an `infra` / eval-harness sprint, not a semantic-touching one.
- **C. `R-uc-cdf-get-customer-context-bot-actual-usage`
  investigation.** The behavioural question Sprint 22 surfaced. An
  investigation-only sprint per Sprint 19's "Bundle-or-defer"
  precedent: walk §3.2 Q3 vs Q5 across the three cases (cs_001 /
  cs_011 / cs_259); decide projection-vs-planner; propose
  remediation as a future bundled sprint. **No bundled fix in the
  investigation sprint itself.**

Sprint 22's recommendation is **B** (the schema extension) as the
narrowest, highest-leverage option: it lifts a known blocker on two
Sprint 21 deferred dispositions, has a clearly-scoped infra surface,
and does not depend on any of the still-open Sprint 19 backlog
items. **A** is the most architecturally consequential but is also
the most expensive; **C** is the natural follow-on to Sprint 22 but
should not jump the line over the schema blocker.

## 11. Risks / known gaps

- **Sprint 21 close narrative carries forward the widening
  framing.** Sprint 21's handoff and the `docs/10-handoff.md` lead
  reference `R-phase2-uc-cdf-customer-context-policy-widen` as a
  recommended next sprint. Sprint 22 closes that R-item but does
  not retroactively edit Sprint 21's archive (sprint archives are
  immutable per `doc_governance.md`). Readers consulting Sprint 21
  directly without reading Sprint 22 first may anchor on the stale
  recommendation. The `docs/10-handoff.md` lead refresh and the
  `action_bank.md` open-questions Sprint-22 note are the mitigation;
  future readers reach the correction through either entry point.
- **Phase 2 front matter is minimal.** The added YAML carries only
  the required `doc_governance.md` schema fields plus the Sprint
  22 `notes:` line. It does not attempt a full audit of phase 2
  vs current runtime (which would be a fold-back-scale task, out
  of scope here). `implementation_status: partial` is the honest
  marking and matches the doc's actual state.
- **The three briefs' Layer hypotheses are unchanged.** Sprint 22
  appended a correction note under the affected anchor in each
  brief but deliberately did not retract the brief authors'
  original Layer hypotheses (e.g. cs_001's
  `R-generator-get-customer-context-policy-mismatch` proposal,
  cs_011's "systematic generator-vs-policy mismatch" framing).
  The dev prompt's stop conditions and the `doc_governance.md`
  notes-style amendment guidance are aligned on this point; a
  future reader walking a brief will see both the original
  reasoning *and* the Sprint 22 correction. This is intentional;
  retracting brief content would require a rewrite, which is
  outside Sprint 22 scope.
- **No new evidence on whether the bot's non-call of
  `get_customer_context` is a real bug.** Sprint 22's premise
  verification confirmed the policy permits the tool; it did not
  attempt to confirm whether the briefs' interpretation of the
  bot's non-call (as a failure to fetch account-state when it
  would have mattered) is correct in each case. That confirmation
  is part of the new R-item's investigation scope.

## 12. Sprint review verdict

Codex returned the §4.2 sprint-close header verdict on 2026-05-14
against the seven Sprint 22 directive deliverables. Verbatim:

```
## Sprint Review Decision
decision: pass
blocking_count: 0
summary: approve (exemption: docs-only - Sprint 15 / 16 / G1 precedent). Review scope `git diff HEAD^..HEAD` is limited to the seven Sprint 22 docs/governance deliverables; no runtime / prompt / eval / judge / override / case-family / FAQ / rubric surface was touched, so the section 4.1 nine-question walk is exempt. Packaging note: untracked deliver artefacts `docs/sprint_objective.md` and `compact/sprint-022-*-prompt.md` were observed in the working tree but are outside the reviewed commit and not blocking.
```

All six scope-check gates passed (per
`docs/sprints/sprint-022-codex-review.md`):

- Gate 1: HEAD^..HEAD touches only the seven expected files.
- Gate 2: phase 2 §2.10.1 byte-identical (no policy widening — only
  the UC-H-local prose reconciliation).
- Gate 3: notes-style correction blocks under the requested anchors;
  original brief text unchanged.
- Gate 4: both closed R-items cite §2.10.1 line 1098 + the two
  corroborating policy surfaces (`docs/customer_service_tool_spec_v0_2.yaml`
  line 260; `docs/current/customer_service_tool_spec_v0_3.md` line 60).
- Gate 5: the new follow-on R-item layer hint is
  `prompt_projection | semantic_planner` (not `product_policy`).
- Gate 6: no override, rubric, judge, runtime, prompt, CaseSpec,
  persona, FAQ, case-family, or brief-quotation edits.

Packaging note resolution: the untracked deliver-agent artefacts
flagged in Codex's summary (`docs/sprint_objective.md` +
`compact/sprint-022-*-prompt.md`) are deliver-agent-owned files
that accumulate outside the dev's commit by design (commit-at-end
workflow). They are bundled into the close commit alongside the
archive moves; this is not a scope-drift event. No A-with-packaging-
note follow-up needed per the prior `feedback_out_of_scope_review_
packaging_rollforward.md` convention (this case is even cleaner:
the verdict was `pass` outright, not `out_of_scope_review`).
