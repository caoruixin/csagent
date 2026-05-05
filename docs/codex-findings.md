## Sprint Review Decision

decision: fix_required
blocking_count: 3
summary: The main Sprint 5.1 case rows now correct the three original P1s, but the correction is not consistent across the required review set. Stale no-prior-search guard guidance remains in skill/design summaries, `current_eval_baseline.md` still names the wrong cs176 expected family, and the Sprint 6 recommendation still lists a fourth stretch action despite the requested 3-action maximum.

## Blocking Diagnostic Failures

- severity: P1
- target case or test: `cs_interactive_259` in `docs/skill_orchestration_candidates.md` and `docs/java_guard_prompt_flexibility_design.md`
- blocks current sprint goal: yes
- exact minimal fix, if any: Remove or rewrite the remaining "F1 C5 + small runtime guard on request_handover(faq_miss) without prior search" guidance. The detailed S1/S3 sections correctly say Sprint 4 r2 already called `search_knowledge`, but the skill comparison / recommended-scope rows still present the old no-prior-search guard as viable. That reintroduces the prior P1 ambiguity.

- severity: P1
- target case or test: `cs_interactive_176` in `docs/current_eval_baseline.md`
- blocks current sprint goal: yes
- exact minimal fix, if any: Update the baseline narrative to match the corrected eval contract. It still says cs176 fails against spec `faq_miss_threshold_exceeded`; the corrected CaseSpec/evidence expects `user_requested`, and Sprint 5.1 correctly says `faq_miss_threshold_exceeded` / `intake_complete_for_uc_k` are not acceptable substitutes.

- severity: P1
- target case or test: Sprint 6 recommendation in `docs/10-handoff.md` / `docs/action_bank.md`
- blocks current sprint goal: yes
- exact minimal fix, if any: Keep the recommended implementation sprint to 3 narrow actions maximum. The corrected list still includes a fourth "(stretch) F1 C5" action and `action_bank.md` still labels the scope "3-4 actions". Since C5 is explicitly downgraded to DISCOVER-side support and not the cs259 primary fix, it should be deferred rather than carried as a Sprint 6 implementation action.

## Non-Blocking Notes

- severity: P2
- target case or test: `cs_interactive_015`
- blocks current sprint goal: no
- exact minimal fix, if any: No fix. It now has exactly one primary layer (`prompt_context_projection`), separate secondary (`skill_orchestration`), and the layer-count summary lists it under prompt/context.

- severity: P2
- target case or test: `cs_interactive_259` detailed rows
- blocks current sprint goal: no
- exact minimal fix, if any: No fix to the detailed case row. It now correctly states `search_knowledge` happened and identifies the missing tail as `resolve_article` / grounded answer / `record_outcome`.

- severity: P2
- target case or test: `cs_interactive_176` C1 detail
- blocks current sprint goal: no
- exact minimal fix, if any: No fix to C1 detail. It now targets preserving / producing `user_requested` when the user explicitly asks for human help and records the residual `active_use_case=UC-I` drift risk.

## Regression Risks

- severity: P2
- target case or test: scope discipline
- blocks current sprint goal: no
- exact minimal fix, if any: The latest diff is docs-only: `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `docs/fix_layer_taxonomy.md`, `docs/prompt_context_projection_audit.md`, and `docs/skill_orchestration_candidates.md`. No Java, prompt template, eval YAML, override, or runtime config change is present.

- severity: P2
- target case or test: stale documentation consumers
- blocks current sprint goal: no
- exact minimal fix, if any: Until the stale baseline/design summaries are corrected, a future sprint could still implement the wrong cs259 guard or accept the wrong cs176 family despite the detailed rows being fixed.

## Recommended Next Sprint Actions

After the diagnostic corrections above, keep Sprint 6 to exactly 3 actions:

1. Kimi `session_create_failed: ReadTimeout` mitigation.
2. Corrected C1 for `cs_interactive_176`, targeting `user_requested`.
3. S1 FAQ-grounded-resolve, with cs259 framed as "search happened, resolve did not complete."

Defer C5/S3 no-prior-search guard language and any fourth stretch action until a later sprint.
