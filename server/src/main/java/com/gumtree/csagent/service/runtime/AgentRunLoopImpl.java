package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.LlmCallEvent;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final LlmInvocationService llmInvocation;
    private final ToolDispatcher toolDispatcher;
    private final ContextProjectionBuilder contextProjectionBuilder;
    private final ActionParser actionParser;

    public AgentRunLoopImpl(LlmInvocationService llmInvocation,
                            ToolDispatcher toolDispatcher,
                            ContextProjectionBuilder contextProjectionBuilder,
                            ActionParser actionParser) {
        this.llmInvocation = llmInvocation;
        this.toolDispatcher = toolDispatcher;
        this.contextProjectionBuilder = contextProjectionBuilder;
        this.actionParser = actionParser;
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
        Map<String, Object> accumulatedToolResults = new LinkedHashMap<>();
        int sequence = 0;
        String lastProjection = null;
        String lastLlmRawResponse = null;

        int maxSteps = Math.max(1, plan.maxToolSteps());

        for (int step = 0; step < maxSteps; step++) {
            // 1. Build plan-aware projection
            String projection;
            try {
                projection = contextProjectionBuilder.build(
                        session, history, plan, userMessage, accumulatedToolResults);
            } catch (Exception ex) {
                log.error("AgentRunLoop projection build failed at step {}: {}",
                        step, ex.getMessage(), ex);
                return AgentRunResult.error("projection_failed: " + ex.getMessage());
            }
            lastProjection = projection;

            // 2. Invoke LLM (LlmInvocationService catches exceptions internally
            //    and returns a safe-escalation response; we still wrap defensively).
            long t0 = System.currentTimeMillis();
            LlmResponse response;
            try {
                response = llmInvocation.invokeChat(projection, userMessage,
                        session.getSessionId(), session.getTotalBotTurns());
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
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
            }

            if (action == null) {
                log.warn("AgentRunLoop parser returned null at step {}; treating raw content as final answer",
                        step);
                String fallbackMessage = (content == null || content.isBlank())
                        ? "I'm having trouble processing your request."
                        : content;
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
            }

            List<ToolCall> calls = action.getToolCalls();
            String userMsg = action.getUserMessage();

            // 5. No tool calls -> final user message (or clarification)
            if (calls == null || calls.isEmpty()) {
                String finalText = (userMsg == null || userMsg.isBlank())
                        ? "I'm looking into this for you."
                        : userMsg;
                return AgentRunResult.finalAnswer(finalText, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
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
                toolEvents.add(new ToolEvent(
                        sequence++, step, base2.toolName(), base2.arguments(),
                        base2.success(), base2.resultData(), base2.errorMessage(),
                        base2.latencyMs()));

                // 6c. Accumulate result so the next iteration sees it.
                if (result != null && result.isSuccess()) {
                    accumulatedToolResults.put(toolName, result.getData());
                } else if (result != null) {
                    Map<String, Object> errorWrap = new LinkedHashMap<>();
                    errorWrap.put("error", result.getErrorMessage());
                    accumulatedToolResults.put(toolName, errorWrap);
                }

                // 6d. Handover short-circuits the loop
                if (HANDOVER_TOOL.equals(toolName)) {
                    handoverRequested = true;
                    handoverReason = extractHandoverReason(call);
                }
            }

            if (handoverRequested) {
                // AgentRunResult.escalate stores escalationReason; the
                // PhaseEvaluator.interpretRunResult mapping applies the
                // customer-facing escalation template. The LLM's userMsg, if
                // any, is captured in the last LlmCallEvent.responseSummary.
                return AgentRunResult.escalate(handoverReason, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
            }
        }

        // Loop exhausted
        log.warn("AgentRunLoop hit max_tool_steps={} without terminal outcome", maxSteps);
        return AgentRunResult.maxSteps(llmEvents, toolEvents, lastProjection);
    }

    private String extractHandoverReason(ToolCall call) {
        if (call == null || call.getArguments() == null) return null;
        Object reason = call.getArguments().get("escalation_reason");
        return reason == null ? null : reason.toString();
    }
}
