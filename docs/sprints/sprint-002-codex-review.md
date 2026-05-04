## Sprint Review Decision

decision: pass
blocking_count: 0
summary: Both Sprint 2.1 P1 blockers are fixed: cs014 now uses the approved v2 override/audit path, and cs014 has a focused ControlKernel/AgentRunLoop persistence regression. The named Sprint 2 guard checks remain green for the reviewed target cases; remaining cs014 live-eval UC drift is the already-deferred D11 item, not a Sprint 2.1 closure blocker.

## Blocking Sprint Failures

None.

## Non-Blocking Notes

### cs014 override/audit path is now reproducible

- severity: P2
- target case or test: `eval_interactive/case_specs/smoke/cs_interactive_014.yaml`, `eval_interactive/case_spec_overrides.yaml`, `qa-reports/case-spec-generation-audit.md`, `qa-reports/smoke-case-review.md`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 2.1. `eval_interactive/case_spec_overrides.yaml` has an approved v2 entry for `source_session_id: 570Q5000008u9gjIAA` with reviewer, date, source, confidence, supporting turns, and rationale; the override sets `expected.escalation_trigger: faq_miss_threshold_exceeded`. The smoke YAML has the same trigger, the audit records `case-level override applied: YES` with changed expected fields, and the smoke review marks cs014 as `needs_override (applied)` with the same recommended trigger.

### cs014 route/loop persistence coverage is stronger than helper-level coverage

- severity: P2
- target case or test: `server/src/test/java/com/gumtree/csagent/integration/Cs014RouteAndLoopHandoverIntegrationTest.java`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 2.1. The new focused integration test starts from cs014's form context with UC-C committed, replays the relevant handover-driving follow-up turn through `ControlKernel.processMessage` and `AgentRunLoopImpl`, asserts `active_use_case=UC-C`, and pairwise asserts consistency across `session.escalationReason`, persisted `request_handover.arguments.escalation_reason`, and `HandoverPayloadAssembler` output. Local targeted run: `mvn -pl server -Dtest=Cs014RouteAndLoopHandoverIntegrationTest test` passed, 1 test / 0 failures.

### Sprint 2 guard checks remain green for the reviewed targets

- severity: P2
- target case or test: `eval_interactive/results/20260504-172751/results.json`, `eval_interactive/results/20260504-172942/results.json`, `eval_interactive/results/20260504-173601/results.json`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 2.1. The latest targeted and smoke result files have zero `L1:escalation_reason_consistency` failures; cs002 reaches `user_distress` when it gets a completed bot turn; cs029 remains UC-D with `user_requested` and no target-case `CONTRACT_VIOLATION:active_use_case`; cs095 does not route to UC-K; and cs066 remains UC-K in both smoke runs.

## Regression Risks

### Live cs014 evals still show deferred UC drift

- severity: P2
- target case or test: `cs_interactive_014`
- blocks current sprint goal: no
- exact minimal fix, if any: No Sprint 2.1 fix required. The latest targeted/smoke evals still show bot-LLM UC drift away from UC-C, but the two P1 blockers under review were the spec override/audit path and deterministic route/loop persistence coverage. Carry D11 forward after closure if the next sprint targets real bot-loop UC stability.

### One smoke run did not exercise cs095 routing because session creation timed out

- severity: P2
- target case or test: `cs_interactive_095` in `eval_interactive/results/20260504-172942/results.json`
- blocks current sprint goal: no
- exact minimal fix, if any: No Sprint 2.1 fix required. The second smoke run exercises the route and records UC-A, not UC-K; keep timeout/retry robustness as a non-blocking follow-up outside this review.

## Recommended Next Sprint Actions

- Carry forward D11 for cs014 real bot-loop UC drift: preserve the UC-C strong-prior signal through the bot-turn loop so live evals stop rotating to UC-B/UC-F while retaining the Sprint 2.1 persistence contract.
