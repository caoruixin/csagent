## Sprint Review Decision

decision: fix_required
blocking_count: 2
summary: Sprint 6 landed G0/G1/G2-shaped changes and kept the implementation mostly inside the requested files and architecture, but the sprint does not meet the acceptance bar. G0 shipped two listed ReadTimeout mitigations instead of exactly one, and G1 still lacks cs176 runtime/eval evidence showing an explicit human-help cue produces `user_requested`.

## Blocking Sprint Failures

- severity: P1
- target case or test: G0 ReadTimeout mitigation
- blocks current sprint goal: yes
- evidence: `docs/sprint_objective.md` says to pick one narrow ReadTimeout mitigation. The implementation and handoff explicitly picked the "widen + accept-and-retry" pair: `AgentClient.create_session` uses a 120s read timeout and retries once on `httpx.ReadTimeout`; `test_agent_client_session_create_timeout.py` pins both behaviours. Auth failures remain non-retryable, the retry loop is bounded, and no secret logging was introduced, but the "exactly one" G0 constraint is not met.
- exact minimal fix: Choose one mitigation. Either keep the 120s create-session timeout and remove the ReadTimeout retry loop/tests, or keep the single ReadTimeout retry and restore the create-session read timeout to the pre-sprint value. Keep the ReadTimeout classification/tagging and the 401/403 non-retry coverage.

- severity: P1
- target case or test: G1 cs_interactive_176
- blocks current sprint goal: yes
- evidence: The prompt snapshot coverage is focused and contains the required `user_requested` tiebreaker language, but the latest runtime/eval evidence does not prove the target behaviour. Targeted cs176 r1 (`results/20260505-112016/results.json`) still fails with `active_use_case=UC-I`, `escalation_reason=service_degraded`, and `L1:escalation_compliance`; targeted cs176 r2 (`results/20260505-112118/results.json`) fails with `active_use_case=UC-K`, `escalation_reason=intake_complete_for_uc_k`, and `L1:escalation_compliance`. Smoke r1/r2 repeat `service_degraded` / `intake_complete_for_uc_k`. The targeted transcripts do not reach the explicit "phone number / talk to someone" seed, so Sprint 6 has no runtime/eval result demonstrating that an explicit human-help cue preserves or produces `user_requested`.
- exact minimal fix: Add one focused cs176 runtime/eval regression that forces the explicit human-help cue into the transcript and asserts the first `request_handover` reason is `user_requested`. If that focused run still emits `service_degraded`, `intake_complete_for_uc_k`, `payment_dispute_detected`, or `faq_miss_threshold_exceeded`, apply the narrowest prompt/runtime correction only for explicit human-help request reason selection. Keep the documented UC-I drift deferral separate.

## Non-Blocking Notes

- severity: P2
- target case or test: G2 cs_interactive_192
- blocks current sprint goal: no
- exact minimal fix, if any: No Sprint 6 blocker. Targeted and smoke evidence now show `search_knowledge` and `resolve_article` before handover, and `L1:source_citation_present` is green; the remaining `correct_outcome=resolve` gap is outside the Sprint 6 G2 acceptance.

- severity: P2
- target case or test: G2 cs_interactive_259
- blocks current sprint goal: no
- exact minimal fix, if any: Add focused result/report evidence for the `search_knowledge` `faq_miss` flag and hit count when documenting that no `resolve_article` was needed. The targeted result currently shows `['classify_use_case', 'search_knowledge', 'search_knowledge', 'request_handover']`; the no-viable-hit explanation is plausible and covered by unit tests, but the result artifact does not expose the tool result fields needed to audit that classification directly.

- severity: P2
- target case or test: focused Sprint 6 regression tests
- blocks current sprint goal: no
- exact minimal fix, if any: None. `mvn -pl server -Dtest=SystemPromptUserRequestedTiebreakerTest,AgentRunLoopS1FaqGroundedResolveGuardTest test` passed 18/18. `python -m pytest -p no:capture tests/test_agent_client_session_create_timeout.py -q` passed 7/7; plain pytest segfaulted in this local conda pytest capture startup path, so `-p no:capture` was required.

## Regression Risks

- severity: P2
- target case or test: cs002 Sprint 6 smoke r2
- blocks current sprint goal: no
- exact minimal fix, if any: Keep the r2 `TIMEOUT` classified separately from G0 `ReadTimeout`. Smoke r1 is green for cs002 (`UC-C` / `user_distress`), and smoke r2 has no `INFRA:ReadTimeout` or `session_create_failed`, but the session-level timeout should remain visible as a separate latency risk if it recurs.

- severity: P2
- target case or test: regression guard set
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 6. Canonical smoke r1 keeps `L1:escalation_reason_consistency` at 0, cs014 at UC-C, cs066 at UC-K, cs095 not UC-K, cs002 distress reconciliation green, and cs029 at UC-D + `user_requested`.

## Recommended Next Sprint Actions

None until the Sprint 6 blockers above are fixed and the sprint can close cleanly.
