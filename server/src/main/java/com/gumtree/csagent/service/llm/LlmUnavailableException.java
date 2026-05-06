package com.gumtree.csagent.service.llm;

/**
 * Sprint 8.1 §M2 — thrown when the LLM provider chain (primary + fallback)
 * cannot complete a chat call due to an infrastructure failure: read /
 * connect timeouts, transient transport errors, HTTP {@code 5xx} or
 * {@code 429} after retry exhaustion, or HTTP {@code 401 / 403}. Distinct
 * from {@link LlmDeadlineExceededException} — the wall-clock budget may
 * still be intact, but the underlying call will not succeed inside the
 * remaining time without operator intervention.
 *
 * <p>Surfaced honestly through the AgentRunLoop / ControlKernel chain as
 * {@code TerminalOutcome.LLM_UNAVAILABLE}; the K0 missing-UC fallback
 * deliberately does NOT fire on this outcome so the trace shows the real
 * failure mode rather than a synthetic UC stamp.
 */
public class LlmUnavailableException extends RuntimeException {

    private final String failureClass;

    public LlmUnavailableException(String message, String failureClass, Throwable cause) {
        super(message, cause);
        this.failureClass = failureClass;
    }

    public String getFailureClass() {
        return failureClass;
    }
}
