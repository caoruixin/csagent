## Sprint Review Decision

decision: fix_required
blocking_count: 2
summary: The cs176 baseline correction is now consistent, and the primary Sprint 6 recommendation lists the right three actions. Two documentation consistency blockers remain: stale cs259 "C5 + small Java guard may suffice" summaries still exist, and "3-4 actions" wording remains in docs that describe the next sprint scope.

## Blocking Diagnostic Failures

- severity: P1
- target case or test: `cs_interactive_259` documentation consistency in `docs/10-handoff.md` and `docs/action_bank.md`
- blocks current sprint goal: yes
- evidence: `docs/10-handoff.md:1729` still lists S3 with anchor `cs_259` and says "F1 §C5 + small Java guard may suffice"; `docs/action_bank.md:715` still says S3 is "mostly covered by F1 §C5 + small Java guard".
- exact minimal fix, if any: Rewrite those summary rows to match the corrected diagnostic: Sprint 4 r2 already called `search_knowledge`; cs259 r2 failed because `resolve_article` / grounded answer / `record_outcome` did not complete; S1 FAQ-grounded-resolve is the primary cs259 fix; C5 is DISCOVER-side support only; S3 / no-prior-search guard is deferred and not the cs259 fix.

- severity: P1
- target case or test: Sprint 6 scope wording in `docs/10-handoff.md` and `docs/action_bank.md`
- blocks current sprint goal: yes
- evidence: `docs/10-handoff.md:1920` still says "Next implementation sprint can be scoped to 3-4 actions"; `docs/action_bank.md:781` still says each sprint "must name 3-4 accepted actions". This contradicts the Sprint 5.2 corrected recommendation that Sprint 6 is capped at exactly 3 actions.
- exact minimal fix, if any: Remove or rewrite the remaining "3-4 actions" wording so the docs consistently say Sprint 6 has exactly 3 actions and no fourth stretch item.

## Non-Blocking Notes

- `docs/current_eval_baseline.md` now aligns cs176 with the eval contract: it says the CaseSpec expects `user_requested`, rejects `faq_miss_threshold_exceeded`, `intake_complete_for_uc_k`, `service_degraded`, and `payment_dispute_detected` as substitutes, and records the residual UC-I drift risk (`docs/current_eval_baseline.md:248`, `docs/current_eval_baseline.md:276`, `docs/current_eval_baseline.md:312`).
- The dedicated skill comparison and F3 design summaries are corrected: `docs/skill_orchestration_candidates.md:404` says the no-prior-search guard is not viable Sprint 6 scope for cs259, and `docs/java_guard_prompt_flexibility_design.md:181` ships exactly one skill, S1.
- The primary Sprint 6 recommendation blocks in `docs/10-handoff.md:1816` and `docs/action_bank.md:734` list exactly the requested three actions and explicitly defer C5 / S3.

## Regression Risks

- Stale summary rows can still steer a future sprint toward implementing the wrong cs259 guard even though the detailed rows are correct.
- The residual "3-4 actions" wording can reintroduce the fourth stretch action ambiguity that Sprint 5.2 was meant to remove.
- Scope discipline is otherwise clean: `HEAD~1..HEAD` changes only docs (`docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `docs/current_eval_baseline.md`, `docs/java_guard_prompt_flexibility_design.md`, `docs/skill_orchestration_candidates.md`). No Java, prompt template, eval YAML, override, qa-report, or broad implementation change is present.

## Recommended Next Sprint Actions

Do not start broader implementation from this handoff until the two residual doc inconsistencies above are corrected. After that, Sprint 6 should remain exactly:

1. Kimi `session_create_failed: ReadTimeout` mitigation.
2. Corrected C1 for `cs_interactive_176`, targeting `user_requested`.
3. S1 FAQ-grounded-resolve, with cs259 framed as "search happened, resolve did not complete."

C5 and the S3 no-prior-search guard stay deferred.
