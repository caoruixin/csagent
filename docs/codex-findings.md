## Validation Review Decision

decision: fix_required
blocking_count: 1
summary: The clean validation runs were executed under working Kimi 2.6 credentials and the prior 401-contaminated Sprint 7 / 7.1 results were not promoted. The Sprint 7 target evidence is mostly classified correctly, but clean smoke r2 contains a non-contaminated `CONTRACT_VIOLATION:active_use_case` on cs259, so the regression guard cannot be reported as green.

## Blocking Validation Failures

- severity: P1
- target case or test: `eval_interactive/results/20260505-225708/results.json` (`sprint7-clean-r2`) / `cs_interactive_259` / `CONTRACT_VIOLATION:active_use_case`
- evidence: The r2 artifact is labeled clean Kimi 2.6 validation and contains no `ReadTimeout`, `INFRA:ReadTimeout`, `session_create_failed`, auth, endpoint, quota, or session-level timeout failure. The cs259 row has `status=CONTRACT_VIOLATION`, `stop_reason=contract_violation`, empty `active_use_case`, and failure tag `CONTRACT_VIOLATION:active_use_case`. `docs/10-handoff.md` reports this as a Kimi tool-use variance flake while also saying all regression guards are green; the Sprint 7 guard allows this exception only for clearly contaminated, non-promoted results, which this is not.
- exact minimal fix or rerun condition: Stop classifying the r2 contract guard as green. Re-run clean smoke with the same Kimi 2.6 setup and require 0 `CONTRACT_VIOLATION:active_use_case` across both smoke runs before passing validation. If cs259 repeats the contract violation, apply a narrow runtime hardening so the cs259 session-create/classify path commits a valid `active_use_case` before contract evaluation, then rerun targeted cs259 plus two clean smokes.

## Residual Failure Classification

- case: `cs_interactive_259` (targeted `results/20260505-224423/results.json` and smoke r1 `eval_interactive/results/20260505-224809/results.json`)
- observed failure: UC-F is now committed, but the bot escalates with `faq_miss_threshold_exceeded`; expected outcome remains `resolve`.
- classification: faq_corpus_gap
- evidence: `active_use_case=UC-F`, `correct_uc=1.0`, tool sequence `['search_knowledge', 'classify_use_case', 'request_handover']`, and `correct_outcome=0.0` because actual outcome is `escalate`. This supports the handoff classification that the Sprint 7 routing objective improved toward UC-F and the remaining r1/targeted failure is answerability, not routing.
- recommended owner: FAQ corpus / eval governance owner

- case: `cs_interactive_015`
- observed failure: Clean targeted, r1, and r2 route to UC-A instead of UC-FP.
- classification: deferred_scope
- evidence: The clean results show `active_use_case=UC-A`; the handoff correctly explains that the live cs015 form has no `ad_id`, so moderation/listing context is not populated and the router sees `moderation_status: unknown`. The context-available UC-FP tiebreaker is covered by focused Java tests, but live cs015 does not exercise that condition.
- recommended owner: runtime routing owner, only as a narrow follow-up description-keyword moderation cue

- case: `cs_interactive_066`
- observed failure: UC-K intake still fails the eval `no_stall` gate in targeted and smoke; r1 ends with `turn_budget_exhausted`, while r2/targeted reach `intake_complete_for_uc_k`.
- classification: judge_volatility
- evidence: UC-K remains stable in all clean runs. Targeted and r2 stamp `intake_complete_for_uc_k` only after the transcript includes the required UC-K fields, while failures are `STALL_AFTER_TOOL_INTENT` from the eval stall detector. This supports the handoff's intake-state classification, not a premature-intake runtime bug.
- recommended owner: eval governance owner for stall detector calibration and persona pacing

- case: `cs_interactive_095`
- observed failure: Still fails expected `resolve`, but routes UC-A and escalates with `faq_miss_threshold_exceeded`.
- classification: product_policy_gap
- evidence: Both clean smokes keep `active_use_case=UC-A`; it is not UC-K and not UC-FP. The remaining issue is that the FAQ/product surface cannot ground the expected email-sync visibility resolution.
- recommended owner: product policy / FAQ corpus owner

- case: `cs_interactive_176`
- observed failure: Still routes to UC-I with `service_degraded` instead of the expected UC-E path.
- classification: deferred_scope
- evidence: The smoke behavior is unchanged from the already-deferred UC-I drift. The focused cs176 explicit-human-help regression remains covered by the 659-test Maven suite and is not reopened by clean validation.
- recommended owner: future narrow runtime routing sprint only if explicitly reprioritized

- case: `cs_interactive_192`
- observed failure: UC-B path escalates with `faq_miss_threshold_exceeded` while expected outcome is `resolve`.
- classification: faq_corpus_gap
- evidence: Clean smoke retains the Sprint 6 G2 shape with `search_knowledge` and `resolve_article` attempts before handover; the remaining failure is answerability, not an uncited-answer or no-resolve regression.
- recommended owner: FAQ corpus / eval governance owner

- case: `cs_interactive_038` (r2 only)
- observed failure: r2 fails with `STALL_AFTER_TOOL_INTENT` / `turn_budget_exhausted` after r1 passed.
- classification: judge_volatility
- evidence: The flip is run-to-run stall-detector variance on an intake case, matching the cs066 pattern.
- recommended owner: eval governance owner

- case: L3 `relevance` / `tone_appropriateness` across otherwise passing cases
- observed failure: L3 tags remain common in both clean smokes.
- classification: judge_volatility
- evidence: r1 has 11 `L3:relevance` and 9 `L3:tone_appropriateness` tags; r2 has 9 of each. These tags affect many cases that otherwise satisfy L1/L2 contracts.
- recommended owner: eval governance / judge calibration owner

## Regression Risks

- severity: P2
- target case or test: cs015 no-`ad_id` rejected-ad shape
- exact minimal fix, if any: If pursued after the P1 contract guard is clean, add only the narrow `description_keyword_signal` moderation cue described in the handoff and preserve cs095 as UC-A / not UC-FP.

- severity: P2
- target case or test: cs066 / cs038 intake stall detection
- exact minimal fix, if any: Calibrate `STALL_AFTER_TOOL_INTENT` so legitimate required-field clarification turns are not treated as stalls when the intake path is progressing.

- severity: P2
- target case or test: FAQ-grounded resolve for cs259 / cs192 / cs095
- exact minimal fix, if any: Audit whether the expected `resolve` labels have resolve-grade FAQ/product-policy support; add corpus coverage or adjust governance labels through the approved path.

## Recommended Next Phase

Narrow Sprint 8 Runtime Fix

Choose a narrow Sprint 8 because clean r2 has a non-contaminated P1 contract-guard failure on the I0 cs259 target. Keep the scope limited to hardening or revalidating the cs259 active-use-case contract path; do not expand into broad review or broad eval expansion.
