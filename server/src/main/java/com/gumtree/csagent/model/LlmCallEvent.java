package com.gumtree.csagent.model;

/**
 * One LLM invocation inside an {@code AgentRunLoop} iteration. Per Phase 3
 * §3.3.3, every loop step records exactly one {@link LlmCallEvent} so the
 * trace UI / replay tooling can reproduce the model↔tool exchange sequence.
 *
 * <p>{@code stepIndex} is the 0-based position within the loop;
 * {@code sequenceIndex} is a loop-local ordering value that may diverge from
 * {@code stepIndex} once interim messages (Phase E) are interleaved with LLM
 * calls. In D16.A both are derived from the step index.
 */
public record LlmCallEvent(
        int sequenceIndex,
        int stepIndex,
        String model,
        int promptTokens,
        int completionTokens,
        long latencyMs,
        String responseSummary
) {

    /**
     * Build an event from an {@link LlmResponse} produced at the given loop
     * step. Truncates the response content to a 500-char summary so the event
     * stays cheap to persist (the full content lives in {@code llm_call_log}).
     *
     * <p>The {@code model} field of {@code LlmResponse} is not currently
     * populated, so this factory leaves it {@code null} — the caller (typically
     * {@code AgentRunLoopImpl}) supplies the active model name when it
     * flattens events into the call log.
     */
    public static LlmCallEvent of(int step, LlmResponse response, long latency) {
        if (response == null) {
            return new LlmCallEvent(step, step, null, 0, 0, latency, null);
        }
        return new LlmCallEvent(
                step,
                step,
                null,
                response.getPromptTokens(),
                response.getCompletionTokens(),
                latency,
                truncate(response.getContent(), 500)
        );
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
