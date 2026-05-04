## Sprint Review Decision

decision: fix_required
blocking_count: 1
summary: E1 and E2 are supported by the current specs, transcript evidence, runtime guard, and Sprint 4 eval outputs. E3 is not fully closed because `qa-reports/smoke-case-review.md` is stale against the final smoke YAML / override / audit state for Sprint 4 support changes and the current smoke fixture set.

## Blocking Sprint Failures

- severity: P1
- target case or test: E3 override / audit consistency, `qa-reports/smoke-case-review.md`
- blocks current sprint goal: yes
- evidence: Sprint 4 formalized `cs_interactive_011` and `cs_interactive_066` through approved overrides, and final smoke YAML plus generation audit agree on the changed expected fields: `cs_interactive_011` is UC-D with `escalation_trigger=faq_miss_threshold_exceeded`; `cs_interactive_066` is UC-K with `escalation_trigger=intake_complete_for_uc_k`. The smoke review report still says `cs_interactive_011` is `user_requested` and `cs_interactive_066` is UC-E / `clarification_budget_exhausted`. The latest `HEAD~1..HEAD` diff also removes `eval_interactive/case_specs/smoke/cs_interactive_004.yaml` and adds `cs_interactive_176.yaml`, while the smoke review report still includes `cs_interactive_004` and has no `cs_interactive_176` review. This violates Sprint 4 E3's requirement that smoke-case-review agree with final expected fields.
- exact minimal fix: Update `qa-reports/smoke-case-review.md` from the current smoke fixture set and approved override state: replace the stale `cs_interactive_004` entry with `cs_interactive_176`; update `cs_interactive_011` to UC-D escalate with `faq_miss_threshold_exceeded`; update `cs_interactive_066` to UC-K escalate with `intake_complete_for_uc_k`; update summary counts and notable follow-up text accordingly.

## Non-Blocking Notes

- severity: P2
- target case or test: E1 `cs_interactive_001` / `cs_interactive_002`
- blocks current sprint goal: no
- exact minimal fix, if any: No fix. The runtime path is narrow and evidence-based: cs001's persona / seeds show calm confusion and keep the spec at `clarification_budget_exhausted`, while cs002's sustained-frustration / all-caps wording justifies `user_distress`. `Cs001LlmDistressGateIntegrationTest` covers the downgrade, deterministic B1 preservation, and `user_requested` precedence.

- severity: P2
- target case or test: E2 `cs_interactive_029`
- blocks current sprint goal: no
- exact minimal fix, if any: No fix. The UC-D CaseSpec override is supported by the account-locked / can't-advertise / callback transcript evidence and Phase 2's UC-D account-login policy. Sprint 4 r1/r2 both show `active_use_case=UC-D`, `escalation_reason=user_requested`, no contract warnings, and passing `correct_uc`.

## Regression Risks

- severity: P2
- target case or test: E3 report consistency guard
- blocks current sprint goal: no
- exact minimal fix, if any: After the immediate report fix, consider adding a small regression check that `qa-reports/smoke-case-review.md` case headings and recommended outcomes match the current smoke fixture set for cases with approved overrides. The current `test_smoke_yaml_matches_override_pipeline_output` protects generated smoke YAML expected fields, but it does not detect stale review-report rows.

## Recommended Next Sprint Actions

- If future sprints rely on `qa-reports/smoke-case-review.md` as closure evidence, make review-report regeneration or a report-vs-smoke consistency check part of the case-spec acceptance path.
