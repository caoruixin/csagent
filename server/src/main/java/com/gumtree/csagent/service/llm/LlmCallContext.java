package com.gumtree.csagent.service.llm;

/**
 * Thread-scoped wall-clock deadline for LLM calls on the current request thread.
 *
 * <p>Sprint 8 §C: any user-facing path that calls an LLM should set a deadline
 * (typically ~10s) at the HTTP entry point. The LLM clients
 * ({@link OpenAiCompatibleLlmClient}, {@link FallbackLlmClient}) check the
 * remaining budget before each attempt / before engaging the cross-provider
 * fallback. When the budget is exhausted, the call aborts with
 * {@link LlmDeadlineExceededException} rather than continuing to retry — the
 * user is better served by a graceful give-up than by waiting for a slow
 * provider tail.
 *
 * <p>Internal / batch / eval paths (no human staring at a screen) leave the
 * deadline unset; the LLM clients then behave as before — bounded only by
 * per-attempt timeouts and the retry / fallback chain.
 *
 * <p>Caveat: {@link ThreadLocal} does not propagate to worker threads in a
 * pool (e.g. the rerank fan-out). For now this is acceptable — the rerank
 * pool's own per-attempt timeouts bound those calls, and the orchestrating
 * thread's deadline still bounds the wait on its {@code Future.get(...)}
 * downstream. If pool-thread propagation becomes important later, switch to
 * an explicit {@code Runnable} wrapper that captures the deadline.
 */
public final class LlmCallContext {

    private static final ThreadLocal<Long> DEADLINE_EPOCH_MILLIS = new ThreadLocal<>();

    private LlmCallContext() {}

    /** Set the wall-clock deadline for LLM calls on the current thread. */
    public static void setDeadline(long epochMillis) {
        DEADLINE_EPOCH_MILLIS.set(epochMillis);
    }

    /** Clear the deadline. MUST be called in a finally block by whoever set it. */
    public static void clear() {
        DEADLINE_EPOCH_MILLIS.remove();
    }

    /**
     * @return milliseconds remaining until the deadline, or {@code null} if
     *         no deadline is set on the current thread. May be negative when
     *         the deadline has already passed.
     */
    public static Long remainingMillis() {
        Long deadline = DEADLINE_EPOCH_MILLIS.get();
        if (deadline == null) {
            return null;
        }
        return deadline - System.currentTimeMillis();
    }

    /** True if a deadline is set and has been exceeded. */
    public static boolean isExceeded() {
        Long remaining = remainingMillis();
        return remaining != null && remaining <= 0;
    }
}
