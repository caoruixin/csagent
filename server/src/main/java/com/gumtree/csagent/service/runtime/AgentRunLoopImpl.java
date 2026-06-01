package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.LlmCallEvent;
import com.gumtree.csagent.model.LlmCallRecord;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.llm.LlmDeadlineExceededException;
import com.gumtree.csagent.service.llm.LlmUnavailableException;
import com.gumtree.csagent.service.runtime.skill.DispatchContext;
import com.gumtree.csagent.service.runtime.skill.RejectVerdict;
import com.gumtree.csagent.service.runtime.skill.SkillGuardrailDispatcher;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link AgentRunLoop} implementation. Per Phase 3 §3.3.3 and Phase 4
 * §D16.B.2, the loop:
 *
 * <ol>
 *   <li>Builds a plan-aware projection (PhasePlan + accumulated tool results)</li>
 *   <li>Invokes the LLM</li>
 *   <li>Records an {@link LlmCallEvent} with a globally-monotonic sequence index</li>
 *   <li>Parses the response for {@code tool_calls} / {@code user_message}</li>
 *   <li>If no tool calls: returns {@link AgentRunResult#finalAnswer}</li>
 *   <li>If tool calls: validates each against {@link PhasePlan#allowedTools()},
 *       dispatches via {@link ToolDispatcher}, accumulates results, and loops</li>
 * </ol>
 *
 * <p>Special cases:
 * <ul>
 *   <li>{@code request_handover} short-circuits to {@link AgentRunResult#escalate}</li>
 *   <li>parser failure: treat raw response content as final user message</li>
 *   <li>LLM exception: caught upstream by {@link LlmInvocationService} which
 *       returns a safe-escalation response, so the parser sees a handover and
 *       the loop terminates normally. As an additional safety net we still
 *       catch any leaked exception and return {@link AgentRunResult#error}.</li>
 *   <li>Tool dispatch exception: caught here and recorded as a failed
 *       {@link ToolEvent}; loop continues so the LLM can react.</li>
 *   <li>Loop hits {@link PhasePlan#maxToolSteps()} without termination:
 *       returns {@link AgentRunResult#maxSteps}</li>
 * </ul>
 *
 * <p>Sequence numbers are monotonic across {@link LlmCallEvent} and
 * {@link ToolEvent} so trace UIs can replay the loop in order.
 */
@Slf4j
@Service
public class AgentRunLoopImpl implements AgentRunLoop {

    private static final String HANDOVER_TOOL = "request_handover";
    private static final String RECORD_OUTCOME_TOOL = "record_outcome";

    /**
     * Sprint 8.1 §M3 — DISCOVER classification phase boundary. When the
     * LLM successfully calls this tool inside a DISCOVER plan and
     * {@link com.gumtree.csagent.service.tools.ClassifyUseCaseTool}
     * commits a non-blank {@code activeUseCase} on the session, the loop
     * returns {@link com.gumtree.csagent.model.TerminalOutcome#USE_CASE_IDENTIFIED}
     * immediately rather than continuing to {@code maxToolSteps} (which
     * the legacy mapper would mis-map to ESCALATE).
     */
    private static final String CLASSIFY_USE_CASE_TOOL = "classify_use_case";
    private static final String DISCOVER_PHASE = "DISCOVER";

    // Sprint 39 (NEW M2) — Sprint 6 §G2 + Sprint 7 §I2 + Sprint 11 §M1
    // reject-reason labels + FAQ_PATH_UCS / FAQ_MISS_REASON / SEARCH_TOOL /
    // RESOLVE_TOOL constants previously inlined here are now owned by
    // SkillGuardrailDispatcher (per Sprint 37 freeze §9 unified dispatcher).
    // The canonical reject-reason labels remain byte-for-byte equivalent and
    // are surfaced via RejectVerdict.predicateName() at the dispatch sites.

    private final LlmInvocationService llmInvocation;
    private final ToolDispatcher toolDispatcher;
    private final ContextProjectionBuilder contextProjectionBuilder;
    private final ActionParser actionParser;
    private final ObjectMapper objectMapper;
    /**
     * Sprint 39 (NEW M2) — unified Skill terminal-predicate dispatcher per
     * Sprint 37 freeze §9. Replaces the 3 {@code shouldRejectXxx} static
     * predicates previously inlined here. Nullable for tests that don't
     * exercise guardrail enforcement (the dispatch sites null-check before
     * invoking).
     */
    private final SkillGuardrailDispatcher skillGuardrailDispatcher;

    /**
     * Spring DI constructor — Spring autowires the {@link SkillGuardrailDispatcher}
     * shipped in Sprint 39 so all four canonical guardrail types fire at the
     * three migrated dispatch sites.
     */
    @Autowired
    public AgentRunLoopImpl(LlmInvocationService llmInvocation,
                            ToolDispatcher toolDispatcher,
                            ContextProjectionBuilder contextProjectionBuilder,
                            ActionParser actionParser,
                            ObjectMapper objectMapper,
                            SkillGuardrailDispatcher skillGuardrailDispatcher) {
        this.llmInvocation = llmInvocation;
        this.toolDispatcher = toolDispatcher;
        this.contextProjectionBuilder = contextProjectionBuilder;
        this.actionParser = actionParser;
        this.objectMapper = objectMapper;
        this.skillGuardrailDispatcher = skillGuardrailDispatcher;
    }

    /**
     * Sprint 39 — backward-compatible 5-arg constructor preserved for the
     * many existing tests that exercise AgentRunLoop infrastructure
     * (deadline, parsing, clarification detection, etc.) and do not need
     * guardrail enforcement. Passing a null dispatcher disables guardrail
     * enforcement; tests that DO exercise predicate semantics
     * (Sprint11ProgressiveResolveTest, Sprint7IntakeStateTest,
     * AgentRunLoopS1FaqGroundedResolveGuardTest) use the 6-arg constructor
     * with {@code SkillTestFixtures.productionDispatcher()}.
     */
    public AgentRunLoopImpl(LlmInvocationService llmInvocation,
                            ToolDispatcher toolDispatcher,
                            ContextProjectionBuilder contextProjectionBuilder,
                            ActionParser actionParser,
                            ObjectMapper objectMapper) {
        this(llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser,
                objectMapper, null);
    }

    @Override
    public AgentRunResult run(PhasePlan plan,
                              BotSession session,
                              String userMessage,
                              List<BotTurn> history) {
        if (plan == null) {
            log.error("AgentRunLoop.run called with null plan; returning error");
            return AgentRunResult.error("plan_required");
        }

        List<LlmCallEvent> llmEvents = new ArrayList<>();
        List<ToolEvent> toolEvents = new ArrayList<>();
        // Sprint 51 / M5 S2 — observation-only side-record: one
        // LlmCallRecord per successful LLM invocation in this run, carrying
        // the full raw response + the projection that fed THIS step (the
        // existing `lastProjection`/`lastLlmRawResponse` get overwritten by
        // the loop and only the final value survives into BotTurn's single
        // columns). Never read by the loop; passed through AgentRunResult to
        // TraceWriter for persistence in `bot_turn_llm_calls`.
        List<LlmCallRecord> llmCallRecords = new ArrayList<>();
        Map<String, Object> accumulatedToolResults = new LinkedHashMap<>();
        // Sprint 067 / S-Auto-12 (A1 idempotency 回挡, backstop half) —
        // per-run identity cache: (toolName | canonicalArgumentsHash) ->
        // the successful ToolEvent that first served that exact call this
        // run. Only success==true dispatches are recorded (so an
        // external-failure result is never cached and a legitimate retry
        // re-dispatches). A byte-identical repeat returns the cached result
        // without re-executing the tool — the deterministic backstop behind
        // the `already_called` binding soft signal. Map lifetime is exactly
        // one run() invocation, so it cannot leak across sessions/turns.
        Map<String, ToolEvent> successfulDispatchCache = new LinkedHashMap<>();
        int sequence = 0;
        String lastProjection = null;
        String lastLlmRawResponse = null;

        int maxSteps = Math.max(1, plan.maxToolSteps());

        // Sprint 7.1 §J0 — persist partial intake fields from the current
        // user turn + form context BEFORE the first projection so the
        // intake_state surface reflects what the user has already supplied.
        // Without this merge, ContextProjectionBuilder would only see fields
        // the LLM persisted via request_handover.arguments.intake_fields,
        // and a normal clarification turn would leave the projection
        // showing every required field as still-remaining (cs066 stall
        // shape). Runs once per user turn since userMessage / formContext
        // are stable across loop iterations.
        mergePartialIntakeFromContext(session, plan, userMessage);

        for (int step = 0; step < maxSteps; step++) {
            // 1. Build plan-aware projection.
            //
            // Sprint 20 Track B (R-prompt-projection-already-called-soft-
            // signal): pass the per-run toolEvents list to the projection
            // builder so it can surface an `already_called` slot listing
            // every successful prior tool dispatch in this run.
            //
            // Sprint 067 / S-Auto-12 (M-Auto-3, A1 hybrid): the slot is now
            // a BINDING soft signal — the system prompt tells the LLM that
            // byte-identical repeats are auto-deduplicated and waste a turn,
            // so it should draft from accumulated_tool_results or take the
            // next action (it still owns WHICH tool / WHAT content). The
            // deterministic backstop half of the hybrid lives at the
            // dispatch site below (`successfulDispatchCache`): a byte-
            // identical success==true repeat is served from cache without
            // re-executing the tool. Soft-signal-alone was empirically
            // falsified (Sprint 19 chose soft-first, Sprint 20 shipped the
            // slot, the identical-retry storm persisted at temp=0), so both
            // halves ship together.
            String projection;
            try {
                projection = contextProjectionBuilder.build(
                        session, history, plan, userMessage, accumulatedToolResults,
                        toolEvents);
            } catch (Exception ex) {
                log.error("AgentRunLoop projection build failed at step {}: {}",
                        step, ex.getMessage(), ex);
                return AgentRunResult.error("projection_failed: " + ex.getMessage());
            }
            lastProjection = projection;

            // 2. Invoke LLM. Sprint 8.1 §M2: deadline / infra failures now
            //    surface as typed exceptions (LlmInvocationService no longer
            //    converts them into a synthetic SAFE_ESCALATION_RESPONSE).
            //    Catch each one and return a TerminalOutcome that the K0
            //    fallback gate can recognise as "no real LLM work happened" —
            //    accumulated llmEvents / toolEvents are preserved so the
            //    trace still reflects what ran before the failure.
            long t0 = System.currentTimeMillis();
            LlmResponse response;
            try {
                response = llmInvocation.invokeChat(projection, userMessage,
                        session.getSessionId(), session.getTotalBotTurns());
            } catch (LlmDeadlineExceededException ex) {
                log.warn("AgentRunLoop llm deadline exceeded at step {}: {}", step, ex.getMessage());
                return AgentRunResult.deadlineExceeded(
                        "llm_deadline_exceeded: " + ex.getMessage(),
                        llmEvents, toolEvents, lastProjection, llmCallRecords);
            } catch (LlmUnavailableException ex) {
                log.error("AgentRunLoop llm unavailable at step {} (failure_class={}): {}",
                        step, ex.getFailureClass(), ex.getMessage());
                return AgentRunResult.llmUnavailable(
                        "llm_unavailable: failure_class=" + ex.getFailureClass()
                                + " " + ex.getMessage(),
                        llmEvents, toolEvents, lastProjection, llmCallRecords);
            } catch (Exception ex) {
                log.error("AgentRunLoop llm invocation failed at step {}: {}",
                        step, ex.getMessage(), ex);
                return AgentRunResult.error("llm_invocation_failed: " + ex.getMessage());
            }
            long latency = System.currentTimeMillis() - t0;
            lastLlmRawResponse = response == null ? null : response.getContent();

            // 3. Record LlmCallEvent with monotonic sequence index
            LlmCallEvent base = LlmCallEvent.of(step, response, latency);
            llmEvents.add(new LlmCallEvent(
                    sequence++, step, base.model(), base.promptTokens(),
                    base.completionTokens(), base.latencyMs(), base.responseSummary()));

            // 4. Parse the response
            String content = response == null ? null : response.getContent();
            ParsedAction action;
            try {
                action = actionParser.parse(content);
            } catch (Exception ex) {
                log.warn("AgentRunLoop parse failure at step {}: {} — treating raw content as final user_message",
                        step, ex.getMessage());
                String fallbackMessage = (content == null || content.isBlank())
                        ? "I'm having trouble processing your request."
                        : content;
                // Sprint 51 — record this invocation even on parser failure
                // (raw response + projection are still useful for debugging
                // the malformed payload). Empty tool_calls since parsing
                // didn't yield any.
                llmCallRecords.add(new LlmCallRecord(
                        step, "chat", llmInvocation.getModelName(), latency,
                        lastLlmRawResponse, projection, List.of()));
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }

            if (action == null) {
                log.warn("AgentRunLoop parser returned null at step {}; treating raw content as final answer",
                        step);
                String fallbackMessage = (content == null || content.isBlank())
                        ? "I'm having trouble processing your request."
                        : content;
                llmCallRecords.add(new LlmCallRecord(
                        step, "chat", llmInvocation.getModelName(), latency,
                        lastLlmRawResponse, projection, List.of()));
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }

            List<ToolCall> calls = action.getToolCalls();
            String userMsg = action.getUserMessage();

            // Sprint 51 / M5 S2 — at the existing step boundary, snapshot a
            // full-fidelity record of this LLM invocation: stepIndex, call
            // type, model, latency, full raw response, the projection that
            // fed THIS step (vs `lastProjection` which is overwritten next
            // step), and the LLM-issued tool calls observed. Observation
            // only — never read by the loop, never alters control flow or
            // call timing.
            llmCallRecords.add(new LlmCallRecord(
                    step, "chat", llmInvocation.getModelName(), latency,
                    lastLlmRawResponse, projection,
                    calls == null ? List.of() : calls));

            // 5. No tool calls -> final user message (or clarification).
            // Sprint 8.1 follow-up (2026-05-06): distinguish clarifying
            // questions from real final answers so the phase mapper does
            // not eagerly transition RESOLVE → CONFIRM → CLOSE while the
            // bot is still asking the user for required details. The LLM
            // routinely emits no-tool-call clarifications like "Can you
            // confirm the ad ID?" which previously surfaced as
            // FINAL_ANSWER and chained RESOLVE → CONFIRM (turn N) →
            // CLOSE (turn N+1) — ending the chat mid-conversation. The
            // existing PhaseEvaluator.isClarificationTurn helper detects
            // the shape ("?"-suffix or clarifying phrase + no tool
            // calls); we use the same predicate here so the AgentRunLoop
            // and PhaseEvaluator agree on what counts as a clarification.
            if (calls == null || calls.isEmpty()) {
                String finalText = (userMsg == null || userMsg.isBlank())
                        ? "I'm looking into this for you."
                        : userMsg;
                if (isClarificationMessage(finalText)) {
                    return AgentRunResult.clarification(finalText, llmEvents, toolEvents,
                            lastProjection, lastLlmRawResponse, llmCallRecords);
                }
                return AgentRunResult.finalAnswer(finalText, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }

            // 6. Dispatch each tool call
            boolean handoverRequested = false;
            String handoverReason = null;
            for (ToolCall call : calls) {
                String toolName = call == null ? null : call.getName();
                if (toolName == null || toolName.isBlank()) {
                    log.warn("AgentRunLoop received tool_call with no name at step {}", step);
                    continue;
                }

                // 6a. Validate against PhasePlan.allowedTools whitelist
                ToolResult validation = toolDispatcher.validateAgainstPlan(plan, toolName);
                if (validation == null || !validation.isSuccess()) {
                    String reason = validation == null
                            ? "tool_not_in_plan"
                            : validation.getErrorMessage();
                    ToolEvent base2 = ToolEvent.rejected(step, call, reason);
                    toolEvents.add(new ToolEvent(
                            sequence++, step, base2.toolName(), base2.arguments(),
                            base2.success(), base2.resultData(), base2.errorMessage(),
                            base2.latencyMs()));
                    continue;
                }

                // Sprint 39 (NEW M2) — unified Skill terminal-predicate
                // dispatcher per Sprint 37 freeze decision (h) §9. REPLACES the
                // three scattered Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1
                // shouldRejectXxx predicates with a single dispatch surface
                // walking the active Skill's `guardrails[]` in declaration
                // order with short-circuit-on-first-reject semantics (design
                // doc §9.2). The Skill scope checks (FAQ_PATH_UCS / INTAKE_UCS)
                // are now implicit via `applicable_use_cases` on the RESOLVE
                // Skill YAMLs. For RESOLVE-INTAKE Skill, intake_complete_required
                // fires on request_handover; for RESOLVE-FAQ Skill,
                // faq_miss_handover_requires_resolve_attempt fires on
                // request_handover and premature_resolve_outcome_guard +
                // must_cite_source fire on record_outcome.
                if (HANDOVER_TOOL.equals(toolName)
                        && IntakeFieldsRegistry.isIntakeUseCase(plan.useCase())) {
                    // Sprint 7.1 §J0 — persist inline intake fields BEFORE
                    // the guardrail check so a single complete handover call
                    // merging the missing fields is allowed through. Side
                    // effect remains at the dispatch site (not migrated to
                    // the dispatcher, which is pure verdict).
                    persistInlineIntakeFields(session, call);
                }
                if (HANDOVER_TOOL.equals(toolName) && skillGuardrailDispatcher != null) {
                    DispatchContext ctx = new DispatchContext(
                            plan, session, accumulatedToolResults,
                            lastLlmRawResponse, Optional.ofNullable(userMsg));
                    Optional<RejectVerdict> verdict =
                            skillGuardrailDispatcher.checkBeforeDispatch(plan, call, ctx);
                    if (verdict.isPresent()) {
                        RejectVerdict v = verdict.get();
                        log.warn(
                                "AgentRunLoop {} guardrail rejected request_handover for UC '{}' at step {}: {}",
                                v.predicateName(), plan.useCase(), step, v.hint());
                        ToolEvent rejected = ToolEvent.rejected(step, call, v.predicateName());
                        toolEvents.add(new ToolEvent(
                                sequence++, step, rejected.toolName(), rejected.arguments(),
                                rejected.success(), rejected.resultData(), rejected.errorMessage(),
                                rejected.latencyMs()));
                        Map<String, Object> guardWrap = new LinkedHashMap<>();
                        guardWrap.put("error", v.predicateName());
                        guardWrap.put("hint", v.hint());
                        // Sprint 7 §I2 trace shape preserved: surface
                        // `missing_fields` (intake-complete guardrail
                        // verdict) on the LLM-visible accumulated_tool_results
                        // entry so the LLM can ask for the right fields on
                        // the next turn. Other trace fields stay in the
                        // dispatcher verdict trace (logged via v.hint()).
                        Object missing = v.trace().get("missing_fields");
                        if (missing != null) {
                            guardWrap.put("missing_fields", missing);
                        }
                        accumulatedToolResults.put(HANDOVER_TOOL, guardWrap);
                        continue;
                    }
                }

                if (RECORD_OUTCOME_TOOL.equals(toolName) && skillGuardrailDispatcher != null) {
                    Map<String, Object> rcArgs = call.getArguments();
                    Object outcomeObj = rcArgs == null ? null : rcArgs.get("outcome_class");
                    if (outcomeObj == null && rcArgs != null) {
                        outcomeObj = rcArgs.get("outcome");
                    }
                    String outcomeClassArg = outcomeObj == null ? null : outcomeObj.toString();
                    DispatchContext ctx = new DispatchContext(
                            plan, session, accumulatedToolResults,
                            lastLlmRawResponse, Optional.ofNullable(userMsg));
                    Optional<RejectVerdict> verdict =
                            skillGuardrailDispatcher.checkBeforeOutcomePersist(
                                    plan, outcomeClassArg, ctx);
                    if (verdict.isPresent()) {
                        RejectVerdict v = verdict.get();
                        log.warn(
                                "AgentRunLoop {} guardrail rejected record_outcome for UC '{}' at step {} (phase={}): {}",
                                v.predicateName(), plan.useCase(), step,
                                session == null ? "?" : session.getCurrentPhase(), v.hint());
                        ToolEvent rejected = ToolEvent.rejected(step, call, v.predicateName());
                        toolEvents.add(new ToolEvent(
                                sequence++, step, rejected.toolName(), rejected.arguments(),
                                rejected.success(), rejected.resultData(), rejected.errorMessage(),
                                rejected.latencyMs()));
                        Map<String, Object> guardWrap = new LinkedHashMap<>();
                        guardWrap.put("error", v.predicateName());
                        guardWrap.put("hint", v.hint());
                        accumulatedToolResults.put(RECORD_OUTCOME_TOOL, guardWrap);
                        // Sprint 12 §N0 — stamp the guard result on the
                        // session for trace fidelity. Sticky across loop
                        // iterations: the first rejection wins so a later
                        // allowed call does not erase the audit signal.
                        if (session != null && (session.getRecordOutcomeGuardResult() == null
                                || session.getRecordOutcomeGuardResult().isBlank()
                                || "none".equals(session.getRecordOutcomeGuardResult()))) {
                            session.setRecordOutcomeGuardResult("rejected:" + v.predicateName());
                        }
                        continue;
                    }
                }
                // Sprint 12 §N0 — observability: record_outcome calls that
                // pass the guardrails mark the session so the post-loop
                // RECORD_OUTCOME_GUARD event captures the allowed branch
                // for trace fidelity. A later rejection in the same loop
                // overwrites this back to rejected via the branch above.
                if (RECORD_OUTCOME_TOOL.equals(toolName) && session != null
                        && (session.getRecordOutcomeGuardResult() == null
                                || session.getRecordOutcomeGuardResult().isBlank()
                                || "none".equals(session.getRecordOutcomeGuardResult()))) {
                    session.setRecordOutcomeGuardResult("allowed");
                }

                // 6a-bis. Sprint 067 / S-Auto-12 (A1 idempotency 回挡,
                // backstop half) — if this exact call (tool name + canonical
                // arguments hash) already SUCCEEDED earlier in THIS run,
                // serve the cached result instead of re-dispatching. The
                // tool is not re-executed (no external call / budget), the
                // event is annotated `deduplicated` + `original_at_step`,
                // and the cached payload is accumulated so the LLM still
                // sees it under accumulated_tool_results. Read-only repeats
                // (search_knowledge, get_customer_context) dominate the
                // temp=0 identical-retry storm; the terminal / short-circuit
                // tools (request_handover, classify_use_case on DISCOVER)
                // end the run on first success so they can never be cached-
                // then-repeated, and a repeat record_outcome is idempotent
                // by design. No tool-name / UC branching: the key reuses the
                // existing order-insensitive canonicalArgumentsHash for
                // every tool. A "hash_error" arguments hash disables dedup
                // for that call (it always re-dispatches).
                String dedupHash = contextProjectionBuilder.canonicalArgumentsHash(
                        call.getArguments());
                String dedupKey = "hash_error".equals(dedupHash)
                        ? null
                        : toolName + "|" + dedupHash;
                if (dedupKey != null) {
                    ToolEvent cachedHit = successfulDispatchCache.get(dedupKey);
                    if (cachedHit != null) {
                        ToolEvent base3 = ToolEvent.deduplicated(
                                step, call, cachedHit.resultData(), cachedHit.stepIndex());
                        toolEvents.add(new ToolEvent(
                                sequence++, step, base3.toolName(), base3.arguments(),
                                base3.success(), base3.resultData(), base3.errorMessage(),
                                base3.latencyMs(), base3.deduplicated(), base3.originalAtStep()));
                        accumulatedToolResults.put(toolName, cachedHit.resultData());
                        log.info(
                                "AgentRunLoop A1 dedup: byte-identical {} at step {} served from "
                                        + "per-run cache (original_at_step={}); tool not re-dispatched",
                                toolName, step, cachedHit.stepIndex());
                        continue;
                    }
                }

                // 6b. Dispatch and record event
                long tt = System.currentTimeMillis();
                ToolResult result;
                try {
                    result = toolDispatcher.dispatch(toolName, session, call.getArguments());
                } catch (Exception ex) {
                    log.warn("AgentRunLoop tool dispatch failed for '{}' at step {}: {}",
                            toolName, step, ex.getMessage());
                    ToolEvent failed = new ToolEvent(
                            sequence++, step, toolName, call.getArguments(),
                            false, null, "tool_dispatch_exception: " + ex.getMessage(),
                            System.currentTimeMillis() - tt);
                    toolEvents.add(failed);
                    continue;
                }
                long toolLatency = System.currentTimeMillis() - tt;
                ToolEvent base2 = ToolEvent.of(step, call, result, toolLatency);
                ToolEvent recorded = new ToolEvent(
                        sequence++, step, base2.toolName(), base2.arguments(),
                        base2.success(), base2.resultData(), base2.errorMessage(),
                        base2.latencyMs());
                toolEvents.add(recorded);

                // Sprint 067 / S-Auto-12 — only success==true dispatches
                // enter the per-run identity cache, so an external-failure
                // result is never cached and a same-args retry re-dispatches.
                // putIfAbsent keeps the FIRST success as the canonical
                // original_at_step for every later byte-identical repeat.
                if (dedupKey != null && result != null && result.isSuccess()) {
                    successfulDispatchCache.putIfAbsent(dedupKey, recorded);
                }

                // Sprint 12 §N0 — stamp terminal-evidence summary on the
                // session as soon as a record_outcome dispatch lands so
                // every run-loop exit path (final answer, escalate,
                // max_steps, error) leaves the trace evidence consistent.
                // The kernel post-loop helpers
                // ({@code emitResolveDispositionEvent},
                // {@code emitRecordOutcomeGuardEvent}) read these slots,
                // and the projection surfaces them under
                // {@code terminal_evidence}.
                if (RECORD_OUTCOME_TOOL.equals(toolName) && session != null) {
                    session.setRecordOutcomeAttempted(Boolean.TRUE);
                    if (result != null && result.isSuccess()) {
                        session.setRecordOutcomeSucceeded(Boolean.TRUE);
                    } else if (session.getRecordOutcomeSucceeded() == null) {
                        session.setRecordOutcomeSucceeded(Boolean.FALSE);
                    }
                }

                // 6c. Accumulate result so the next iteration sees it.
                if (result != null && result.isSuccess()) {
                    accumulatedToolResults.put(toolName, result.getData());
                } else if (result != null) {
                    Map<String, Object> errorWrap = new LinkedHashMap<>();
                    errorWrap.put("error", result.getErrorMessage());
                    accumulatedToolResults.put(toolName, errorWrap);
                }

                // 6d. Handover short-circuits the loop only when the
                // dispatch actually SUCCEEDED. Sprint 9 §O1 — terminal-state
                // honesty: a failed request_handover (e.g. malformed payload,
                // SalesforceService failure) must not be treated as a
                // successful terminal escalation; the error is surfaced in
                // accumulated_tool_results above so the LLM can retry within
                // the remaining maxToolSteps. If the loop later hits MAX_STEPS
                // without a successful handover, PhaseEvaluator maps that to
                // ESCALATE/turn_budget_exhausted via the canonical mapping —
                // no synthetic success is ever stamped here.
                if (HANDOVER_TOOL.equals(toolName)
                        && result != null && result.isSuccess()) {
                    handoverRequested = true;
                    handoverReason = extractHandoverReason(call);
                }

                // 6e. Sprint 8.1 §M3 — DISCOVER deterministic phase
                // boundary. When classify_use_case successfully commits a
                // non-blank active_use_case on a DISCOVER plan, return
                // USE_CASE_IDENTIFIED immediately. ControlKernel performs
                // a single bounded same-turn replan into RESOLVE for the
                // newly committed UC instead of letting the loop run out
                // its remaining DISCOVER steps (which the legacy mapper
                // mis-maps to ESCALATE / faq_miss_threshold_exceeded even
                // when search_knowledge returned hits and classify
                // succeeded).
                if (CLASSIFY_USE_CASE_TOOL.equals(toolName)
                        && DISCOVER_PHASE.equalsIgnoreCase(plan.phase())
                        && result != null && result.isSuccess()
                        && session != null
                        && session.getActiveUseCase() != null
                        && !session.getActiveUseCase().isBlank()) {
                    log.info(
                            "AgentRunLoop §M3: classify_use_case committed active_use_case={} "
                                    + "on DISCOVER plan; returning USE_CASE_IDENTIFIED for same-turn "
                                    + "replan into RESOLVE",
                            session.getActiveUseCase());
                    return AgentRunResult.useCaseIdentified(
                            session.getActiveUseCase(),
                            llmEvents, toolEvents,
                            lastProjection, lastLlmRawResponse, llmCallRecords);
                }
            }

            if (handoverRequested) {
                // AgentRunResult.escalate stores escalationReason; the
                // PhaseEvaluator.interpretRunResult mapping applies the
                // customer-facing escalation template. The LLM's userMsg, if
                // any, is captured in the last LlmCallEvent.responseSummary.
                return AgentRunResult.escalate(handoverReason, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }
        }

        // Loop exhausted. Sprint 8.2 §M0b — preserve the last LLM raw
        // response on MAX_STEPS so the trace UI does not falsely render
        // "no LLM call for this turn" when multiple successful LLM calls
        // happened before the loop hit maxToolSteps. Terminal outcome and
        // PhaseEvaluator MAX_STEPS mapping are unchanged.
        log.warn("AgentRunLoop hit max_tool_steps={} without terminal outcome", maxSteps);
        return AgentRunResult.maxSteps(llmEvents, toolEvents, lastProjection,
                lastLlmRawResponse, llmCallRecords);
    }

    /**
     * Sprint 7.1 §J0 — merge partial intake field values supplied by the
     * current user turn (and seeded by the form context) into
     * {@link BotSession#getIntakeFields()} so the next
     * {@code intake_state} projection reflects them. No-op for non-intake
     * UCs and for UCs the {@link IntakeFieldExtractor} has no heuristics
     * for. Never overwrites a field already present in the session — the
     * LLM-supplied {@code intake_fields} (via
     * {@link #persistInlineIntakeFields}) always win.
     */
    void mergePartialIntakeFromContext(BotSession session, PhasePlan plan, String userMessage) {
        if (session == null || plan == null) return;
        String uc = plan.useCase();
        if (!IntakeFieldsRegistry.isIntakeUseCase(uc)) return;
        if (!IntakeFieldExtractor.handlesUc(uc)) return;

        Map<String, String> existing = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        Map<String, String> merged = IntakeFieldExtractor.mergeForUc(
                uc, existing, userMessage, session.getFormContext(), objectMapper);
        if (merged == existing || merged.equals(existing)) {
            return;
        }
        try {
            session.setIntakeFields(objectMapper.writeValueAsString(merged));
            log.debug("AgentRunLoop merged partial intake fields for UC {}: collected={}",
                    uc, merged.keySet());
        } catch (Exception ex) {
            log.warn("AgentRunLoop failed to persist partial intake_fields: {}", ex.getMessage());
        }
    }

    /**
     * Sprint 7 §I2 — persist the LLM-supplied {@code intake_fields} payload
     * (when present on a {@code request_handover} call) back to
     * {@link BotSession#getIntakeFields()} so subsequent turns and the
     * {@link IntakeFieldsRegistry#intakeComplete} predicate can see the
     * collected values. Field names are normalised to canonical form via
     * {@link IntakeFieldsRegistry#canonicalFieldName}; only non-blank
     * values are merged. Tolerant — never throws on malformed input.
     */
    void persistInlineIntakeFields(BotSession session, ToolCall call) {
        if (session == null || call == null || call.getArguments() == null) {
            return;
        }
        Object raw = call.getArguments().get("intake_fields");
        if (!(raw instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return;
        }
        Map<String, String> existing = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        @SuppressWarnings("unchecked")
        Map<String, ?> incoming = (Map<String, ?>) rawMap;
        Map<String, String> merged = IntakeFieldsRegistry.mergeFields(existing, incoming);
        if (merged.equals(existing)) {
            return;
        }
        try {
            session.setIntakeFields(objectMapper.writeValueAsString(merged));
        } catch (Exception ex) {
            log.warn("AgentRunLoop failed to persist inline intake_fields: {}", ex.getMessage());
        }
    }

    /**
     * Extract the {@code escalation_reason} argument from a
     * {@code request_handover} tool call (null-safe). Used to compute the
     * outer-loop {@code handoverReason} captured for the run result.
     */
    private String extractHandoverReason(ToolCall call) {
        if (call == null || call.getArguments() == null) return null;
        Object reason = call.getArguments().get("escalation_reason");
        return reason == null ? null : reason.toString();
    }

    // Sprint 39 (NEW M2) — the three Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11
    // §M1 shouldRejectXxx static predicate methods (and the local
    // sessionCollected helper) are REMOVED per Sprint 37 freeze decision (g)
    // §8. Their semantics are migrated into typed handlers on
    // SkillGuardrailDispatcher (handleFaqMissHandoverRequiresResolveAttempt,
    // handleIntakeCompleteRequired, handlePrematureResolveOutcomeGuard
    // — the last preserves the Sprint 11 / 11.1 / 12 frozen
    // ResolveDispositionEvaluator delegation per
    // runtime_freeze_and_risk_policy.md §1.1 #3, only relocating the
    // invocation site).

    /**
     * Sprint 8.1 follow-up — heuristic clarification detection on a
     * no-tool-calls user_message. Mirrors the existing
     * {@link PhaseEvaluator#isClarificationTurn(com.gumtree.csagent.model.ParsedAction)}
     * predicate (kept duplicated here so the AgentRunLoop does not
     * depend on PhaseEvaluator). True when the message ends with
     * {@code '?'} or contains a recognised clarifying phrase
     * ("could you", "can you tell", "what is", "which", "do you have").
     *
     * <p>When this returns {@code true}, the loop emits
     * {@link com.gumtree.csagent.model.TerminalOutcome#CLARIFICATION_NEEDED}
     * so {@link PhaseEvaluator#interpretRunResult} keeps the session in
     * the current phase rather than chaining RESOLVE → CONFIRM → CLOSE
     * mid-conversation.
     */
    static boolean isClarificationMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        String trimmed = userMessage.trim();
        if (trimmed.endsWith("?")) return true;
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("could you")
                || lower.contains("can you tell")
                || lower.contains("what is")
                || lower.contains("which")
                || lower.contains("do you have");
    }
}
