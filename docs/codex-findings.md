## Sprint Review Decision

decision: fix_required
blocking_count: 2
summary: The cs014 expectation correction is semantically plausible and the targeted eval now passes, but Sprint 2.1 is not closed yet. The change bypasses the approved CaseSpec override/audit mechanism, and the new regression is helper-level rather than the requested route/loop persistence test.

## Blocking Sprint Failures

### cs014 CaseSpec correction bypasses the approved override/audit path

- severity: P1
- target case or test: `eval_interactive/case_specs/smoke/cs_interactive_014.yaml`, `eval_interactive/case_spec_overrides.yaml`, `qa-reports/case-spec-generation-audit.md`
- blocks current sprint goal: yes
- evidence: Sprint 2.1 changed `expected.escalation_trigger` directly in `cs_interactive_014.yaml` from `user_distress` to `faq_miss_threshold_exceeded` and added an inline rationale comment. The formal override registry exists, but `eval_interactive/case_spec_overrides.yaml` has no entry for source session `570Q5000008u9gjIAA`; the current generation audit still records cs014 with `trigger=user_distress`, and `qa-reports/smoke-case-review.md` still recommends `UC-C escalate, user_distress`. Phase 5 explicitly says approved overrides must live in `case_spec_overrides.yaml` and direct hand edits to generated YAML are not the correction mechanism.
- exact minimal fix: Add an approved v2 override for `source_session_id: 570Q5000008u9gjIAA` with reviewer/date/source/confidence/supporting turn numbers/rationale and `expected.escalation_trigger: faq_miss_threshold_exceeded`; regenerate or update the audit so the final YAML, override registry, and audit all agree.

### cs014 regression coverage is still not route/loop persistence coverage

- severity: P1
- target case or test: `server/src/test/java/com/gumtree/csagent/service/runtime/Cs014RouteAndDistressRegressionTest.java`
- blocks current sprint goal: yes
- evidence: The new test uses the actual cs014 form text and seed messages, but it only asserts helper behavior: distress phrase detection, all-caps detection, topic aliasing, B2 bias, UC-K negative helper, and reason canonicalization. It does not exercise the `ControlKernel` / AgentRunLoop path, does not replay the observed follow-up turn sequence through persisted turns, and does not assert persisted `active_use_case`, `session.escalationReason`, `request_handover.arguments.escalation_reason`, and handover payload consistency together.
- exact minimal fix: Add one focused case-level integration regression that starts from the cs014 form context, replays the relevant follow-up turns, drives the actual route/loop path to escalation, and asserts `active_use_case=UC-C` plus a consistent persisted semantic reason across session state, `request_handover.arguments.escalation_reason`, and the handover payload.

## Non-Blocking Notes

### cs014 corrected expectation is supported by the observed target run

- severity: P2
- target case or test: `eval_interactive/results/20260504-164044/results.json`
- blocks current sprint goal: no
- exact minimal fix, if any: None for runtime behavior. The targeted cs014 run passes 1/1 with `active_use_case=UC-C`, `escalation_reason=faq_miss_threshold_exceeded`, zero failure tags, and `L1:escalation_reason_consistency` green.

### Sprint 2 target guards remain structurally green in the follow-up evals

- severity: P2
- target case or test: `eval_interactive/results/20260504-163026/results.json`, `eval_interactive/results/20260504-163518/results.json`, `eval_interactive/results/20260504-164044/results.json`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 2.1. B0 has zero `L1:escalation_reason_consistency` failures in the follow-up runs; `cs_interactive_029` still has UC-D plus `user_requested`; `cs_interactive_095` stays off UC-K; `cs_interactive_066` still routes UC-K; and `cs_interactive_002` still reaches `user_distress` when it gets past LLM flake paths.

### Scope discipline was mostly maintained

- severity: P2
- target case or test: latest diff `HEAD~1..HEAD`
- blocks current sprint goal: no
- exact minimal fix, if any: No production rollback needed. The diff is limited to cs014 spec/docs plus one Java regression test; it does not implement broad policy expansion, trace/transcript alignment, judge stabilization, promotion/anchor expansion, or service-outcome taxonomy work.

## Regression Risks

### Regeneration can revert or contradict the hand-edited cs014 expectation

- severity: P2
- target case or test: `eval_interactive/case_spec_overrides.yaml`, `qa-reports/case-spec-generation-audit.md`
- blocks current sprint goal: no
- exact minimal fix, if any: This is resolved by the P1 override fix above. Until then, the smoke YAML and the generation/audit sources disagree, so future regeneration may silently restore `user_distress`.

### cs014 bot-limit terminal reason is not fully stable across smoke reruns

- severity: P2
- target case or test: `cs_interactive_014` in `20260504-163026` and `20260504-163518`
- blocks current sprint goal: no
- exact minimal fix, if any: Decide whether cs014 should require exact `faq_miss_threshold_exceeded` or only the bot-limit family. Follow-up smoke run 1 used `faq_miss_threshold_exceeded`, while run 2 passed with `turn_budget_exhausted`; this no longer reopens the original cross-family `user_distress` blocker, but it should be pinned if exact terminal reasons become a gate.

## Recommended Next Sprint Actions

### Keep a tiny cs014 gate after closure

- severity: P2
- target case or test: `cs_interactive_014`
- blocks current sprint goal: no
- exact minimal fix, if any: After the override/audit and route/loop regression fixes land, keep the targeted cs014 1-case eval as a short gate for one sprint so the corrected expectation does not regress during the next routing work.

### Add a small audit consistency check for edited smoke specs

- severity: P2
- target case or test: `eval_interactive/case_spec_overrides.yaml` and `qa-reports/case-spec-generation-audit.md`
- blocks current sprint goal: no
- exact minimal fix, if any: Add a lightweight check that fails when a smoke CaseSpec expected field differs from generated/audited output without a matching approved override entry.
