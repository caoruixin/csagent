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
 *
 * <p>Sprint 067 / S-Auto-12 (A1 idempotency 回挡): {@code deduplicated}
 * marks an event that was served from the per-run identity cache instead
 * of being re-dispatched (a byte-identical {@code success==true} repeat of
 * an earlier call in the same {@code AgentRunLoop.run(...)}). For such an
 * event the tool was NOT re-executed (no external call / budget);
 * {@code originalAtStep} names the {@code stepIndex} of the original
 * dispatch whose result was reused, and is {@code -1} for every
 * non-deduplicated event. Both fields are observation-only — they annotate
 * the trace and never alter control flow.
 */
public record ToolEvent(
        int sequenceIndex,
        int stepIndex,
        String toolName,
        Map<String, Object> arguments,
        boolean success,
        Object resultData,
        String errorMessage,
        long latencyMs,
        boolean deduplicated,
        int originalAtStep
) {

    public ToolEvent {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    /**
     * Backward-compatible 8-arg constructor (pre-Sprint-067 shape) for the
     * many call sites and tests that construct a normal, non-deduplicated
     * event. Delegates to the canonical constructor with
     * {@code deduplicated=false} and {@code originalAtStep=-1}.
     */
    public ToolEvent(int sequenceIndex, int stepIndex, String toolName,
                     Map<String, Object> arguments, boolean success,
                     Object resultData, String errorMessage, long latencyMs) {
        this(sequenceIndex, stepIndex, toolName, arguments, success, resultData,
                errorMessage, latencyMs, false, -1);
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

    /**
     * Sprint 067 / S-Auto-12 — build an event for a tool call that was
     * served from the per-run identity cache instead of being re-dispatched
     * (a byte-identical {@code success==true} repeat). The tool was not
     * re-executed, so {@code latencyMs} is zero; {@code resultData} is the
     * cached payload from the original dispatch and {@code originalAtStep}
     * is that dispatch's {@code stepIndex}. {@code success} is true because
     * only successful results ever enter the cache.
     */
    public static ToolEvent deduplicated(int step, ToolCall call,
                                         Object cachedResultData, int originalAtStep) {
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                true,
                cachedResultData,
                null,
                0L,
                true,
                originalAtStep
        );
    }
}
