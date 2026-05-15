## Sprint Review Decision
decision: pass
blocking_count: 0
summary: approve (exemption: docs-only - Sprint 15 / 16 / G1 precedent). Review scope `git diff HEAD^..HEAD` is limited to the seven Sprint 22 docs/governance deliverables; no runtime / prompt / eval / judge / override / case-family / FAQ / rubric surface was touched, so the section 4.1 nine-question walk is exempt. Packaging note: untracked deliver artefacts `docs/sprint_objective.md` and `compact/sprint-022-*-prompt.md` were observed in the working tree but are outside the reviewed commit and not blocking.

## Findings

No blocking findings.

## Scope Check

- Gate 1: pass - HEAD^..HEAD touches only `docs/foundational/phase2_domain_realization_spec.md`, `docs/action_bank.md`, the three named Failure Briefs, `docs/sprints/sprint-022-handoff.md`, and `docs/10-handoff.md`.
- Gate 2: pass - phase 2 section 2.10.1 is byte-identical across HEAD^..HEAD; the edit only reconciles the UC-H-local `get_customer_context` prose and adds the requested review metadata.
- Gate 3: pass - each named brief receives a `> **Correction (Sprint 22, 2026-05-14):**` block under the requested anchor; the original brief text is unchanged.
- Gate 4: pass - both closed R-items cite phase 2 section 2.10.1 line 1098 plus `docs/customer_service_tool_spec_v0_2.yaml` line 260 and `docs/current/customer_service_tool_spec_v0_3.md` line 60.
- Gate 5: pass - `R-uc-cdf-get-customer-context-bot-actual-usage` is proposed/deferred with layer hint `prompt_projection` or `semantic_planner`, explicitly not `product_policy`.
- Gate 6: pass - no override, rubric, judge, runtime, prompt, CaseSpec, persona, FAQ, case-family, or brief-quotation surface is edited in the reviewed commit.

## Out-of-scope Deferrals

None raised by this review.
