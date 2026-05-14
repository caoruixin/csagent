## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 24 passes. The reviewed commit range (`6c12ec9..e21b1b6`) ships the Track A event-shape coalesce requested by the sprint objective: `BotSession.consecutiveDeadlineCount`, builder initialization, V13 migration, `PhaseEvaluator` reset/increment/threshold behavior, and a behavior-level regression test covering first deadline, second consecutive deadline, and reset. Track B remains investigation-only in the handoff, includes the coarse-proxy table plus no-per-call-claim paragraph, and proposes `R-per-llm-call-latency-instrumentation` as prerequisite to any future budget or model decision. No semantic hardcode or hard-fence violation was found.

### Blocking Findings

None.

### 4.1 Anti-Hardcode Kernel

Per-PR verdict: approve.

1. Keyword / regex / if-else / enum / per-UC matrix for a semantic decision?
   No. The new branch in `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:789` through `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:803` uses the event-shape counter `consecutiveDeadlineCount` and `deadlineCount < 2`. It does not inspect user text, UC, CaseSpec id, keyword, or regex.
2. If yes to (1), justified by a current Tier-0 invariant?
   N/A. Q1 is no; Sprint 24 adds no Tier-0 invariant.
3. Could the same outcome be achieved by projecting a soft signal to the LLM?
   No. The LLM call has already failed to complete under `DEADLINE_EXCEEDED`; the runtime owns timeout fallback emission.
4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id?
   No. The second-deadline message is generic slow-response wording in `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:797` through `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:801`, and the test is behavior-level, not CaseSpec-id-level.
5. Does the change move semantic ownership from the LLM to Java?
   No. The runtime already owns timeout handling; the change only varies the deterministic fallback message after repeated infra outcomes.
6. Does the change add an if-else block to the prompt?
   No. The commit does not touch `server/src/main/resources/prompts/system_prompt.txt` or prompt projection code.
7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?
   Yes. No tool schema, permission, PII/safety, or grounding path changed.
8. Does the PR ship generalization eval coverage?
   Accepted under the Sprint 24 deviation. Target = `Sprint24DeadlinePlaceholderCoalesceTest.secondConsecutiveDeadline_emitsDistinctHonestNextStepMessage`; neighbor = full server suite attempted; negative = reset behavior in `Sprint24DeadlinePlaceholderCoalesceTest.nonDeadlineOutcomeBetweenDeadlines_resetsCounterAndPlaceholderFiresAgain`; shadow = deferred to G2 as allowed by the objective.
9. If temporary, does it carry an explicit rollback or sunset plan?
   N/A. The counter is a durable infra repair mirroring `runtime_error_count`, not a temporary semantic guard.

### Sprint 24 Checks

- Track A bundle present: `server/src/main/java/com/gumtree/csagent/model/BotSession.java:79`, `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:108`, `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:690`, `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:789`, `server/src/main/resources/db/migration/V13__add_consecutive_deadline_count.sql:1`, and `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint24DeadlinePlaceholderCoalesceTest.java:1`.
- The second-deadline message names the issue and offers next steps: `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:799` through `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:801`.
- The regression test asserts first placeholder, second distinct/non-empty/next-step intent, no auto-handover escalation reason, and reset-to-placeholder behavior.
- Track B shipped no runtime/eval/config code. The handoff includes the coarse-proxy table with `n_cases` and `sum(total_turns)`, cites the objective's `24.6s -> 27.6s` p95 signal, states that case-level `elapsed_ms` is signal not per-call evidence, and proposes `R-per-llm-call-latency-instrumentation`.
- Action-bank disposition is appropriate for Sprint 24 close: `R-slow-llm-placeholder-coalesce-honest-next-step` is closed by Track A, `R-llm-latency-budget-investigation` is reframed to coarse proxy plus the follow-on instrumentation R-item, and `R-cs040-uc-k-topic-subject-routing` remains unchanged.
- Hard fences hold in the reviewed commit range: no deadline-budget widening, no model config change, no prompt projection work, no eval-spec work, no Tier-0 change, and no coarse proxy represented as per-call latency evidence.
- Out-of-scope surfaces are not edited: `server/src/main/java/com/gumtree/csagent/controller/ChatController.java` and the `cs_040` UC-K routing surface are untouched; the handoff names both as out of scope.

### Verification

- `mvn -pl server -Dtest=Sprint24DeadlinePlaceholderCoalesceTest test` passed: 3 tests, 0 failures.
- `mvn -pl server -am test` was run and reproduced the known unrelated prompt failure: 901 tests, 1 failure in `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`. This matches the handoff's pre-existing `system_prompt.txt` note and is not attributed to the Sprint 24 commit.

### Informational Observations

- `docs/sprints/sprint-024-handoff.md` reports a Track B methodology discrepancy: its independent extraction yields larger case-level p50/p95 values than the planning-turn citation, while still citing the required `24.6s -> 27.6s` p95 signal and preserving the no-per-call-evidence caveat. This is correctly carried as a human open question, not a blocking Track B claim.
- The working tree also contains pre-existing/deliver-agent-owned uncommitted files (`docs/sprint_objective.md`, `compact/sprint-024-*.md`, `csagent_system_design_review.md`, and `server/src/main/resources/prompts/system_prompt.txt`). Under the packaging-rollforward rule, these are informational and not scope drift for the Sprint 24 substantive review.
