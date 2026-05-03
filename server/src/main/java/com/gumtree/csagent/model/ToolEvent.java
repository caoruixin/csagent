package com.gumtree.csagent.model;

import com.gumtree.csagent.service.tools.ToolResult;

import java.util.Map;

/**
 * One tool dispatch inside an {@code AgentRunLoop} iteration. Captures the
 * call request (name + arguments), execution outcome (success / error /
 * rejected-by-plan), the result payload, and latency. Per Phase 3 §3.3.3,
 * every {@link ToolEvent} is later flattened into {@code bot_turns.tool_calls}
 * for the trace UI and replay tooling.
 *
 * <p>{@code stepIndex} is the 0-based position within the loop;
 * {@code sequenceIndex} preserves intra-step ordering when an LLM step
 * requests multiple tools at once (e.g. {@code [get_customer_context,
 * search_knowledge]} in a single response).
 */
public record ToolEvent(
        int sequenceIndex,
        int stepIndex,
        String toolName,
        Map<String, Object> arguments,
        boolean success,
        Object resultData,
        String errorMessage,
        long latencyMs
) {

    public ToolEvent {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    /**
     * Build an event from a successful (or tool-side failed) dispatch. The
     * supplied {@code latency} is preferred over {@link ToolResult#getLatencyMs()}
     * because it is measured at the loop boundary (matches how the loop in
     * D16.B will time things).
     */
    public static ToolEvent of(int step, ToolCall call, ToolResult result, long latency) {
        boolean ok = result != null && result.isSuccess();
        Object data = result == null ? null : result.getData();
        String err = result == null ? null : result.getErrorMessage();
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                ok,
                data,
                err,
                latency
        );
    }

    /**
     * Build an event for a tool call that was rejected by
     * {@code ToolDispatcher.validateAgainstPlan(...)} before dispatch. Latency
     * is zero because the tool was never invoked.
     */
    public static ToolEvent rejected(int step, ToolCall call, String reason) {
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                false,
                null,
                reason,
                0L
        );
    }
}
