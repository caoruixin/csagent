## Sprint Review Decision

decision: pass
blocking_count: 0
summary: Sprint 8 closes the Post-Sprint-7 clean-validation P1 blocker. Targeted cs259 and both clean Sprint 8 smoke runs commit `active_use_case=UC-F` for cs259, have 0 `CONTRACT_VIOLATION:active_use_case`, and preserve the single-action K0 scope.

## Blocking Sprint Failures

None.

## Non-Blocking Notes

- severity: P2
- target case or test: targeted `cs_interactive_259` / `results/20260505-234352/results.json`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 8. The case now reaches normal scoring with `active_use_case=UC-F`, `escalation_reason=faq_miss_threshold_exceeded`, and no contract violation. The remaining failure is `correct_outcome` / tool-sequence / citation relevance driven by the unresolved FAQ corpus answerability gap for payment-after-selling.

- severity: P2
- target case or test: Sprint 8 smoke r1/r2 cs259 / `eval_interactive/results/20260505-234448/results.json`, `eval_interactive/results/20260505-235231/results.json`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 8. Both runs show cs259 `active_use_case=UC-F`, `correct_uc=1.0`, and no `CONTRACT_VIOLATION:active_use_case`; residual cs259 failure remains FAQ corpus / answerability.

- severity: P2
- target case or test: scope discipline / `HEAD~1..HEAD`
- blocks current sprint goal: no
- exact minimal fix, if any: None. The implementation surface is limited to `ControlKernel` fallback hardening plus two Sprint 8 regression test classes and closure docs. No cs015 follow-up, cs066 stall calibration, FAQ corpus change, product policy change, cs176 drift fix, S3, S5, judge calibration, broad routing rewrite, or CaseSpec churn appears in the latest diff.

- severity: P2
- target case or test: Sprint 6 / Sprint 7 regression evidence
- blocks current sprint goal: no
- exact minimal fix, if any: None. Handoff evidence reports 23/23 Sprint 8 focused tests, 682/682 Maven tests, and 294/294 eval pytest tests green, including Sprint 7 I1/I2 tests, the Sprint 6 G2 FAQ-grounded-resolve guard, and the cs176 explicit-human-help focused regression.

## Regression Risks

- severity: P2
- target case or test: `ControlKernel.applyMissingUseCaseFallback`
- blocks current sprint goal: no
- exact minimal fix, if any: No current fix. The helper now runs in the AgentRunLoop ESCALATE branch for any missing-UC escalation and uses the existing fallback inference, with UC-F sale-proceeds vocabulary added. Current negative guards cover cs014, cs066, cs095, cs011, cs002, cs029, and cs176; if a future missing-UC escalation is misclassified, gate the fallback more tightly to UNKNOWN/empty-form payment-sale-proceeds text or add the observed shape as a focused negative guard.

- severity: P2
- target case or test: `cs_interactive_015`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 8. cs015 remains UC-A because moderation context is still unavailable when the form lacks `ad_id`; the previously identified description-keyword moderation cue remains a future narrow runtime candidate, not part of K0.

- severity: P2
- target case or test: `cs_interactive_095` r2
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 8. r2 keeps UC-A / not UC-K / not UC-FP, but hits `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP`; handle under Eval Governance stall-detector calibration if it remains important.

- severity: P2
- target case or test: `cs_interactive_176` smoke behavior
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 8. Smoke now routes UC-E in both runs but still fails escalation compliance because the persona seed does not surface explicit human-help; the focused explicit-human-help regression remains green.

## Recommended Next Phase

Eval Governance Sprint

The K0 contract blocker is closed and the remaining failure surface is FAQ corpus answerability, stall/persona volatility, and L3 judge variance. Do not broaden runtime scope before governance work unless a new P0/P1 runtime regression appears.
