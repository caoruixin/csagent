package com.gumtree.csagent.service.llm;

/**
 * Thrown when an LLM call (or its retry / cross-provider fallback) cannot
 * complete within the wall-clock deadline set via {@link LlmCallContext}.
 *
 * <p>Treated as <b>non-transient</b> by {@link FallbackLlmClient}: once the
 * deadline is exceeded there is no point engaging the fallback, since by
 * definition we no longer have time to wait for it. This is the explicit
 * "fail fast and give up" signal the upstream caller should render as a
 * graceful UX message rather than a generic 500.
 */
public class LlmDeadlineExceededException extends RuntimeException {

    public LlmDeadlineExceededException(String message) {
        super(message);
    }
}
