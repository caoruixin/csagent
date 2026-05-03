package com.gumtree.csagent.model;

import java.util.List;
import java.util.Optional;

/**
 * Result of a single {@code AgentRunLoop.run(...)} invocation. Per Phase 3
 * §3.3.3 (v9 — D16). Aggregates every LLM call, tool dispatch, and outbound
 * message produced during the model↔tool exchange so {@code ControlKernel}
 * can flatten them into existing tables ({@code llm_call_log},
 * {@code bot_turns.tool_calls}, {@code bot_turns.bot_response}).
 *
 * <p>Collection components are stored as {@link List#copyOf} so callers can
 * accumulate into mutable lists during the loop and hand them off without
 * worrying about post-construction mutation.
 *
 * <p>{@link #escalationReason()} is only populated when
 * {@link #terminalOutcome()} is {@link TerminalOutcome#ESCALATE}; for other
 * outcomes it is {@link Optional#empty()}.
 *
 * <p>{@link #lastProjection()} is the JSON projection produced for the final
 * LLM iteration. {@code ControlKernel.recordRunResult()} persists it to
 * {@code bot_turns.projected_context} so trace UIs can replay the agent run.
 * It is {@code null} for {@link #error(String)} results.
 *
 * <p>{@link #lastLlmRawResponse()} is the verbatim {@code LlmResponse.content}
 * from the LLM call that produced the terminal outcome (final answer or
 * escalation). Persisted to {@code bot_turns.llm_raw_response}. {@code null}
 * for max-steps / error results.
 */
public record AgentRunResult(
        List<AgentMessage> messages,
        List<ToolEvent> toolEvents,
        List<LlmCallEvent> llmEvents,
        TerminalOutcome terminalOutcome,
        String finalUserMessage,
        Optional<String> escalationReason,
        String lastProjection,
        String lastLlmRawResponse
) {

    public AgentRunResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
        toolEvents = toolEvents == null ? List.of() : List.copyOf(toolEvents);
        llmEvents = llmEvents == null ? List.of() : List.copyOf(llmEvents);
        escalationReason = escalationReason == null ? Optional.empty() : escalationReason;
    }

    public static AgentRunResult finalAnswer(String userMessage,
                                              List<LlmCallEvent> llmEvents,
                                              List<ToolEvent> toolEvents) {
        return finalAnswer(userMessage, llmEvents, toolEvents, null, null);
    }

    public static AgentRunResult finalAnswer(String userMessage,
                                              List<LlmCallEvent> llmEvents,
                                              List<ToolEvent> toolEvents,
                                              String lastProjection,
                                              String lastLlmRawResponse) {
        return new AgentRunResult(
                List.of(AgentMessage.finalMessage(userMessage)),
                toolEvents,
                llmEvents,
                TerminalOutcome.FINAL_ANSWER,
                userMessage,
                Optional.empty(),
                lastProjection,
                lastLlmRawResponse
        );
    }

    public static AgentRunResult maxSteps(List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents) {
        return maxSteps(llmEvents, toolEvents, null);
    }

    public static AgentRunResult maxSteps(List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents,
                                           String lastProjection) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.MAX_STEPS,
                null,
                Optional.empty(),
                lastProjection,
                null
        );
    }

    public static AgentRunResult escalate(String reason,
                                           List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents) {
        return escalate(reason, llmEvents, toolEvents, null, null);
    }

    public static AgentRunResult escalate(String reason,
                                           List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents,
                                           String lastProjection,
                                           String lastLlmRawResponse) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.ESCALATE,
                null,
                Optional.ofNullable(reason),
                lastProjection,
                lastLlmRawResponse
        );
    }

    public static AgentRunResult error(String message) {
        return new AgentRunResult(
                List.of(),
                List.of(),
                List.of(),
                TerminalOutcome.ERROR,
                null,
                Optional.ofNullable(message),
                null,
                null
        );
    }
}
