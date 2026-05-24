package com.gumtree.csagent.model;

import java.util.List;

/**
 * Sprint 51 / M5 S2 — full-fidelity per-step LLM invocation record produced
 * inside {@code AgentRunLoopImpl.run(...)} and persisted, one row per step, into
 * {@code bot_turn_llm_calls} via {@code TraceWriter}. Each step accumulates one
 * record at the existing loop boundary; no change to loop control flow, call
 * count, ordering, timing, or termination semantics.
 *
 * <p>Distinct from {@link LlmCallEvent}, which stores only a 500-char
 * {@code responseSummary} for the cheap per-call observability table; this
 * record carries the full raw response and the step's own projection so the
 * admin trace can render every invocation with its own context.
 *
 * <p>{@code toolCalls} carries the LLM-issued tool calls observed at this step
 * as already-serialized JSON (or {@code null} when none). Parsing remains the
 * responsibility of {@code ActionParser}; this record just snapshots what the
 * LLM emitted on this step for the trace.
 */
public record LlmCallRecord(
        int stepIndex,
        String callType,
        String model,
        long latencyMs,
        String llmRawResponse,
        String projectedContext,
        List<ToolCall> toolCalls
) {

    public LlmCallRecord {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}
