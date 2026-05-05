## Sprint Review Decision

decision: fix_required
blocking_count: 3
summary: Sprint 5 stayed diagnostic-only and the overall layer split is directionally sound, but the diagnostic cannot pass yet. One reviewed case lacks exactly one primary fix layer, and two next-sprint candidates are not aligned with the actual Sprint 4 result evidence / eval contract.

## Blocking Diagnostic Failures

- severity: P1
- target case or test: `docs/fix_layer_taxonomy.md` / `cs_interactive_015`
- blocks current sprint goal: yes
- exact minimal fix, if any: Pick exactly one primary layer for `cs_interactive_015`; keep the other as secondary. The row currently says primary is `prompt_context_projection / skill_orchestration (joint)`, which violates F0's "exactly one primary layer" contract and makes review question 1 fail.

- severity: P1
- target case or test: `cs_interactive_259` evidence in `docs/fix_layer_taxonomy.md`, `docs/prompt_context_projection_audit.md`, `docs/skill_orchestration_candidates.md`, `docs/10-handoff.md`
- blocks current sprint goal: yes
- exact minimal fix, if any: Correct the evidence and candidate action. Sprint 4 r2 did call `search_knowledge` before escalating: tool sequence was `['search_knowledge', 'classify_use_case', 'request_handover']` with `lcs=1/4`. The failure is missing `resolve_article` / grounded resolve / `record_outcome`, not simply "no prior search". A guard that refuses `faq_miss_threshold_exceeded` without prior search would not address the observed r2 failure.

- severity: P1
- target case or test: `cs_interactive_176` / F1 C1 recommended prompt change
- blocks current sprint goal: yes
- exact minimal fix, if any: Align C1's target outcome with the eval contract. The CaseSpec expects `escalation_trigger: user_requested`, and the result fails L1 because actual `payment_dispute_detected` / `service_degraded` is cross-family from `user_requested`. The audit currently says `faq_miss_threshold_exceeded` or `intake_complete_for_uc_k` would be acceptable "family-match against spec `user_requested`"; that is not supported by the result evidence. Either make C1 preserve/produce the `user_requested` family for this case or reclassify the issue.

## Non-Blocking Notes

- severity: P2
- target case or test: Java fixes reserved for true invariants
- blocks current sprint goal: no
- exact minimal fix, if any: No broad implementation request. The F3 boundary is mostly clear, and no new top-level `java_guard` primary is recommended. Be careful with deferred guards in S1/S2/S3: only promote them when phrased as enforceable terminal predicates, not content-aware enum rewrites.

- severity: P2
- target case or test: F1 C2 routing prompt tiebreaker
- blocks current sprint goal: no
- exact minimal fix, if any: Keep deferred as written. The candidate depends on `customer_context.moderation_status`, but the audit also describes `routing_prompt.txt` as topic/description-only. Before this becomes an action, verify that the routing prompt can actually see the moderation status or move the cue to a projection/PhasePlan surface that can.

- severity: P2
- target case or test: F2 skill candidates
- blocks current sprint goal: no
- exact minimal fix, if any: S1 and S2 are concrete enough to become future actions: trigger conditions, tools, state, terminal outcomes, Java boundaries, prompt responsibilities, and eval cases are listed. S3 should stay deferred until the cs259 evidence is corrected.

## Regression Risks

- severity: P1
- target case or test: recommended Sprint 6 scope
- blocks current sprint goal: yes
- exact minimal fix, if any: Do not carry forward the C5/S3 "no prior search" guard as a cs259 fix until the evidence is corrected. It risks adding a Java guard that passes tests while leaving the observed failure unchanged.

- severity: P2
- target case or test: `cs_interactive_176`
- blocks current sprint goal: no
- exact minimal fix, if any: C1 may reduce `payment_dispute_detected` picks in r1, but it does not address the r2 `active_use_case=UC-I` drift. Treat the r2 UC drift as residual risk in the next sprint acceptance criteria.

- severity: P2
- target case or test: diagnostic-only scope
- blocks current sprint goal: no
- exact minimal fix, if any: Claude appears to have avoided broad implementation during Sprint 5. The handoff lists docs-only changes, and the current tracked worktree shows no Java, prompt, eval YAML, or runtime config diff from Sprint 5.

## Recommended Next Sprint Actions

After the diagnostic corrections above, keep Sprint 6 to 3 narrow actions:

1. Kimi `session_create_failed: ReadTimeout` mitigation.
2. Corrected C1 for `cs_interactive_176`, explicitly targeting the `user_requested` family or reclassifying the case before implementation.
3. S1 FAQ-grounded-resolve skill, with cs259 framed as "search happened, resolve did not complete" and cs192 framed as "answer emitted without citation / resolve sequence incomplete."

Defer C5/S3 until the cs259 evidence is rewritten. Defer C2 and S2 as already recommended unless the next sprint explicitly swaps them in.
