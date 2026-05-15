# D16 AgentRunLoop QA Report

**Generated**: 2026-04-27
**Scope**: D16.A (scaffolding) + D16.B (RESOLVE/FAQ) + D16.C (RESOLVE/INTAKE) + D16.D (DISCOVER/CONFIRM/CLOSE/ESCALATE)
**Tech Stack**: Spring Boot 3 / Java 17 (Maven, JUnit 5, Mockito)
**Verdict**: **SHIP** with two LOW-severity follow-ups noted below.

---

## 1. Build Status

| Metric | Value |
|---|---|
| Command | `mvn clean verify -pl server` |
| Exit code | 0 (BUILD SUCCESS) |
| **Tests run** | **391** |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

Authoritative line from Surefire summary:

```
[INFO] Results:
[INFO] Tests run: 391, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The promised 391-test pass count is confirmed exactly.

---

## 2. Architectural Boundaries (Task B)

### B.1 PhaseEvaluator new-path methods do not call LLM/tools/knowledge directly — **PASS**

Inspected `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`:

- `plan()` — lines 250–452. Only reads session state, builds and returns `PhasePlan` instances. No direct calls to `llmInvocation`, `toolDispatcher`, or `knowledgeSearchService`.
- `interpretRunResult()` — lines 464–518. Pure mapping function: reads `AgentRunResult`/`PhasePlan`/`BotSession`, returns `PhaseTransitionDecision`. No infrastructure calls.
- `mapFinalAnswer()` — lines 526–581. Private helper invoked by `interpretRunResult`; pure switch over `fromPhase`. Clean.
- `intakeCompleteTrigger()` and `buildIntakeSystemInstruction()` — pure helpers used only by `plan()`. Clean.

Grep for `llmInvocation|toolDispatcher|knowledgeSearchService` in the file shows the first usage at line 598 (`evaluateDiscover`) and beyond — all inside the legacy methods. The new-path surface is verifiably free of direct LLM/tool/knowledge calls.

### B.2 Every `toolDispatcher.dispatch` is preceded by `validateAgainstPlan`; rejections produce `ToolEvent.rejected` and continue — **PASS**

Inspected `AgentRunLoopImpl.java` lines 167–224:

- Line 175: `ToolResult validation = toolDispatcher.validateAgainstPlan(plan, toolName);`
- Lines 176–186: On rejection (`!validation.isSuccess()`), a `ToolEvent.rejected(...)` is recorded and the inner `for` loop falls through to `continue`. The outer `for (step...)` is not aborted.
- Line 192: `result = toolDispatcher.dispatch(...)` is reached only after `validateAgainstPlan` succeeded.

There is exactly one `dispatch` call site in the file, and it is gated on a successful validation in the immediately-preceding code.

This is also covered by `AgentRunLoopImplTest.toolNotInPlan_isRejectedAndLoopContinues` which proves at the JVM level that a rejected tool produces a failed `ToolEvent` and the loop proceeds to a second LLM iteration that emits a final answer.

### B.3 All 6 route keys produce non-null PhasePlans — **PASS**

| Route key | Source location | Plan returned | Test coverage |
|---|---|---|---|
| `RESOLVE_FAQ` (RESOLVE + UC-A/…/UC-FP) | `plan()` lines 414–451 | non-null | `PhaseEvaluatorPlanTest.plan_resolveFaqUc_returnsFullPlan` |
| `RESOLVE_INTAKE` (RESOLVE + UC-G/H/I/J/K) | `plan()` lines 372–411 | non-null | `plan_resolveIntake{UcG,UcH,UcI,UcJ,UcK}…` (5 tests) |
| `DISCOVER` | `plan()` lines 256–282 | non-null | `plan_discoverPhase_returnsDiscoverPlan` + null-UC variant |
| `CONFIRM` | `plan()` lines 287–313 | non-null | `plan_confirmPhase_returnsConfirmPlan` |
| `CLOSE` | `plan()` lines 317–335 | non-null | `plan_closePhase_returnsClosePlan` |
| `ESCALATE` | `plan()` lines 338–363 | non-null | `plan_escalatePhase_nonIntakeUc…` + intake variant |

Every route key returned by `ControlKernel.computeRouteKey` (lines 556–575) maps to a branch in `PhaseEvaluator.plan()` that builds and returns a non-null `PhasePlan`. The unit tests in `PhaseEvaluatorPlanTest` exercise each branch directly, so no additional tests need to be added.

The only `null` returns from `plan()` are intentional legacy-fallback signals: unrecognized phase, `RESOLVE` with null/blank UC, and unknown UC in registry — all asserted by `plan_nullActiveUc_returnsNull` and `plan_unknownUcInRegistry_returnsNull`.

### B.4 Loop bounds are enforced — **PASS**

Inspected `AgentRunLoopImpl.run()`:

- Line 91: `int maxSteps = Math.max(1, plan.maxToolSteps());` — `maxToolSteps=0` is silently coerced to 1 to guarantee at least one LLM invocation; negative values cannot occur because `PhasePlan`'s compact constructor rejects them.
- Line 93: `for (int step = 0; step < maxSteps; step++)` — strict half-open loop. With `maxToolSteps=2` the loop body runs for `step=0` and `step=1` only.
- Line 122–125: Exactly one `LlmCallEvent` is appended per iteration before any tool dispatch, so the LLM is invoked at most `maxSteps` times.
- Lines 237–238: After loop exhaustion, `AgentRunResult.maxSteps(...)` is returned.

`AgentRunLoopImplTest.maxSteps_returnsMaxStepsOutcome` (line 159) exercises this with `maxToolSteps=3` and an LLM that always asks for tools, asserting:
- `terminalOutcome == MAX_STEPS`
- `llmEvents.size() == 3`
- `toolEvents.size() == 3`

The promised behaviour ("with `maxToolSteps=2` and an always-ask-tools LLM, returns `MAX_STEPS` after exactly 2 iterations") is structurally identical to that test, just at a different bound. The bound is enforced correctly.

---

## 3. Behavioral Regression (Task C)

### C.1 Legacy path with empty feature flag — **PASS (verified by code review + property test)**

- `application.yml` (production baseline) ships `agent.run-loop.enabled-phases: []`.
- `AgentRunLoopPropertiesTest.defaults_areEmptyEnabledPhasesAndFourMaxSteps` confirms the empty default binds correctly.
- `ControlKernel.processMessage` line 131–133: routing through `AgentRunLoop` is gated on `agentRunLoopProperties.getEnabledPhases().contains(routeKey)`. With an empty list, the condition is false for every route, so execution falls through to the legacy `phaseEvaluator.evaluate(...)` at line 191.

The legacy path's behavior in this build is byte-identical to D14 (no production code in the legacy methods has been modified — only additive changes in new methods). All pre-existing tests for legacy `evaluateDiscover/evaluateResolve/evaluateConfirm/evaluateClose/evaluateEscalate` continue to pass within the 391-test suite.

The team did not add a dedicated end-to-end "legacy path with empty flag" test, but this is acceptable because:
1. Every existing pre-D16 test exercises the legacy path (since they all use `new AgentRunLoopProperties()` with default empty list, or the legacy path directly).
2. The routing decision is a single `if` check whose negative branch is the original code path.

### C.2 Integration tests — **PASS**

| Test | Result |
|---|---|
| `AgentRunLoopAd1002IntegrationTest.ad1002_endToEnd_returnsGroundedAnswerInSingleHttpResponse` | passing — verifies AD-1002 grounded answer in a single HTTP turn with two LLM calls + one `get_customer_context` dispatch + RESOLVE→CONFIRM transition |
| `AgentRunLoopIntakeIntegrationTest.ucH_endToEnd_collectsIntakeAndEscalatesWithCase` | passing — verifies UC-H two-turn flow ending in ESCALATE with `create_case_controlled` + `request_handover` |
| `AgentRunLoopIntakeIntegrationTest.ucH_searchKnowledgeAttempt_isRejectedByValidateAgainstPlan` | passing — verifies `search_knowledge` is *never* dispatched in INTAKE even when LLM (mis)attempts it |
| `AgentRunLoopConfirmCloseIntegrationTest.confirmTurn_userSatisfied_recordOutcomeThenCloses` | passing — verifies CONFIRM → CLOSE with `record_outcome` |

Surefire log shows each integration test class running cleanly with the expected log lines:
```
ControlKernel — Session sess-ad1002: routing turn through AgentRunLoop (route=RESOLVE_FAQ, uc=UC-A)
ControlKernel — Session sess-ad1002: phase transition RESOLVE -> CONFIRM (reason: answer_provided)
…
ControlKernel — Session sess-uc-h-1: routing turn through AgentRunLoop (route=RESOLVE_INTAKE, uc=UC-H)
ControlKernel — Session sess-uc-h-1: phase transition RESOLVE -> ESCALATE (reason: agent_escalated)
…
ControlKernel — Session sess-confirm-1: routing turn through AgentRunLoop (route=CONFIRM, uc=UC-A)
ControlKernel — Session sess-confirm-1: phase transition CONFIRM -> CLOSE (reason: user_satisfied)
```

### C.3 Smoke eval — **SKIPPED**

A live eval would require booting the Spring server with a working DashScope-compatible LLM endpoint, which is outside the QA harness scope and would produce non-deterministic comparisons against the `20260426-042904` baseline. The unit + integration test coverage above is sufficient to validate the refactor without an end-to-end eval. Recommend running the eval as a next step in a developer environment before promoting to staging.

---

## 4. Code Review Findings

### Severity summary

| HIGH | MEDIUM | LOW |
|---|---|---|
| 0 | 0 | 2 |

No HIGH or MEDIUM issues. Two LOW-severity observability nits.

---

### LOW-1: `bot_turns.bot_response` is null for MAX_STEPS / ERROR escalations

- **File**: `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:511`
- **Description**: When `AgentRunLoop` returns `MAX_STEPS` or `ERROR`, `AgentRunResult.finalUserMessage()` is `null` (see `AgentRunResult.maxSteps(...)` and `AgentRunResult.error(...)` factories). `recordRunResult` writes that null directly into `BotTurn.botResponse`, but `ControlKernel.processMessage` (line 143–146) substitutes a friendly escalation template (`"I'm having difficulty resolving this..."` / `"I'm experiencing a technical issue..."`) before returning to the user. As a result, the trace UI and `/v1/demo/sessions/{id}/trace` endpoint will show a null `bot_response` for the very turn where the user did see an escalation message.
- **Code**:
  ```java
  // ControlKernel.recordRunResult
  BotTurn turn = BotTurn.builder()
      .botResponse(result.finalUserMessage())   // null for MAX_STEPS / ERROR
      …
  ```
- **Suggestion**: Pass `responseText` (the value actually emitted to the user) as a separate argument into `recordRunResult`, or have `interpretRunResult` mutate `AgentRunResult` to backfill `finalUserMessage`. Either approach makes the persisted trace match user-facing reality. Not a blocker — escalation reason and tool events are still captured — but useful for replay/debug fidelity.

### LOW-2: `lastLlmRawResponse` is null for MAX_STEPS results even though a final LLM response exists

- **File**: `server/src/main/java/com/gumtree/csagent/model/AgentRunResult.java:88` and `AgentRunLoopImpl.java:238`
- **Description**: The `AgentRunResult.maxSteps(...)` factory hard-codes `lastLlmRawResponse=null`. However, the loop body keeps `lastLlmRawResponse` updated on every iteration (line 119), and at the moment we exit via the bottom of the for-loop the variable holds the final iteration's content. That value is discarded and the persisted `bot_turns.llm_raw_response` is null. The data is still recoverable via `llm_call_log` (which is auto-populated by `LlmInvocationService`), so this is purely a redundancy / convenience gap.
- **Suggestion**: Add a `lastLlmRawResponse` parameter to `AgentRunResult.maxSteps` (mirroring the existing `lastProjection` parameter) and pass `lastLlmRawResponse` from `AgentRunLoopImpl` line 238. Trivial change, improves trace fidelity.

---

### Targeted Code Review answers (Task D)

1. **Thread safety** — **PASS**. `AgentRunLoopImpl` is a Spring `@Service` (singleton). All mutable state (`llmEvents`, `toolEvents`, `accumulatedToolResults`, `sequence`, `lastProjection`, `lastLlmRawResponse`) is local to `run()`. The injected collaborators (`LlmInvocationService`, `ToolDispatcher`, `ContextProjectionBuilder`, `ActionParser`) are themselves singletons whose existing thread-safety predates D16. No shared mutable fields are introduced.

2. **Error handling** — **PASS**. Every catch block in `AgentRunLoopImpl.run()` logs at WARN or ERROR and either returns a typed `AgentRunResult.error(...)` (catastrophic failures: null plan, projection failure, LLM exception) or records a `ToolEvent` with the error message and continues (recoverable failures: parse error, missing tool name, dispatch exception). No silent swallows. `ControlKernel.recordRunResult` catches at the persistence boundary and logs at ERROR — acceptable, mirrors the legacy `recordTurn`.

3. **Resource leaks** — **PASS**. The loop holds no `AutoCloseable` resources (no streams, no JDBC handles, no background futures). All HTTP/JDBC resources are managed by their respective Spring components.

4. **Feature flag boundary (mid-session switch)** — **PASS**. The flag is checked per-turn via `agentRunLoopProperties.getEnabledPhases().contains(routeKey)`. Session state in the database (`current_phase`, `active_use_case`, etc.) is the only mid-session contract; both legacy and new paths consume and update it identically. A session that runs turn 1 on the legacy path and turn 2 on the new path will see consistent phase transitions because both paths agree on the phase-transition vocabulary (`RESOLVE → CONFIRM` for FAQ, `RESOLVE → ESCALATE` for INTAKE-complete, etc.). Note: an *in-flight* `processMessage` call cannot switch — it commits to one path at line 131.

5. **`PhasePlan` immutability** — **PASS**. Compact constructor (`PhasePlan.java:40–53`) defensively copies every collection-valued component (`List.copyOf` for `allowedTools`, `Set.copyOf` for `requiredContextKeys`, `Collections.unmodifiableSet(new LinkedHashSet<>(…))` for `validTerminalOutcomes`). Callers cannot mutate the record after construction.

6. **Trace/observability completeness** — **PASS with the LOW-1/LOW-2 caveats above**. Per-iteration LLM calls are auto-persisted to `llm_call_log` by `LlmInvocationService` (line 92, 101), so `/v1/demo/sessions/{id}/llm-calls` shows every LLM call that the loop made. Tool events are flattened into `bot_turns.tool_calls` JSONB by `ControlKernel.recordRunResult` with `sequence_index` preserved, so `/v1/demo/sessions/{id}/trace` shows the full model↔tool exchange in order. Both endpoints will correctly reflect multi-iteration runs. The two LOW issues above only affect what's stored as the *bot response* on the bot_turns row; the loop-level events are complete.

---

## 5. Eval Smoke Comparison

**Skipped.** Running the interactive eval requires a live Spring server with a configured DashScope-compatible LLM endpoint, which is out of scope for this QA pass and would produce a non-deterministic comparison. Recommend a developer-side eval comparing this build against `results/20260426-042904` before promoting to staging.

---

## 6. Recommendation

### Verdict: **SHIP**

Justification:
- Build is green (391/391 tests pass, exactly as promised).
- All four architectural boundaries promised in the design (PhaseEvaluator new path is infra-free, every dispatch is plan-validated, all 6 route keys produce non-null plans, loop bounds are enforced) are verifiably enforced in the code and exercised by tests.
- Three integration tests (AD-1002 FAQ, UC-H INTAKE with rejection, CONFIRM→CLOSE) cover the end-to-end flow for the most representative phase-route combinations.
- Legacy path is preserved by an empty default feature flag in `application.yml`; existing tests continue to validate it.
- `PhasePlan` is a defensively-copied immutable record. `AgentRunLoopImpl` is stateless. No HIGH/MEDIUM concerns.

Two LOW-severity follow-ups (LOW-1, LOW-2) are observability/fidelity gaps for `MAX_STEPS` / `ERROR` outcomes. Neither blocks shipping. Recommend opening tickets and addressing in a follow-on cleanup commit.

Pre-staging recommendation: run the interactive smoke eval in a dev environment to triangulate against the `20260426-042904` baseline before promoting to staging traffic.
