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

    /**
     * Sprint 8.1 §M1 follow-up (2026-05-06) — shared HTTP-attempt budget for
     * the whole provider chain on the current request thread. Initialised by
     * {@link #setDeadline(long)} to {@link #DEFAULT_GLOBAL_ATTEMPT_BUDGET},
     * decremented once per HTTP attempt by both
     * {@link OpenAiCompatibleLlmClient} (per attempt) and
     * {@link FallbackLlmClient} (before engaging the fallback). Result: with
     * a user-facing deadline in effect, the total number of HTTP calls
     * across primary + fallback is hard-capped at 2 — closing the
     * regression where two primary attempts (24 s of read timeouts) plus a
     * fallback attempt blew past the 30 s wall-clock budget. When no
     * deadline is set (internal / batch / eval / unit-test paths), the
     * counter is null and the existing per-client retry behaviour stands.
     *
     * <p>Stored as a single-element {@code int[]} so the holder can be
     * mutated in place without re-installing the {@link ThreadLocal} on
     * every decrement.
     */
    private static final ThreadLocal<int[]> GLOBAL_ATTEMPT_BUDGET = new ThreadLocal<>();

    /**
     * Default global HTTP-attempt budget when a user-facing deadline is set.
     * Sized to fit one primary + one fallback attempt within the 30 s
     * wall-clock budget the controller installs.
     */
    public static final int DEFAULT_GLOBAL_ATTEMPT_BUDGET = 2;

    private LlmCallContext() {}

    /**
     * Set the wall-clock deadline for LLM calls on the current thread. Also
     * resets the shared HTTP-attempt budget to
     * {@link #DEFAULT_GLOBAL_ATTEMPT_BUDGET} so primary + fallback share a
     * single bounded retry budget.
     */
    public static void setDeadline(long epochMillis) {
        DEADLINE_EPOCH_MILLIS.set(epochMillis);
        GLOBAL_ATTEMPT_BUDGET.set(new int[]{DEFAULT_GLOBAL_ATTEMPT_BUDGET});
    }

    /** Clear the deadline. MUST be called in a finally block by whoever set it. */
    public static void clear() {
        DEADLINE_EPOCH_MILLIS.remove();
        GLOBAL_ATTEMPT_BUDGET.remove();
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

    /**
     * @return remaining HTTP attempts allowed across the whole provider
     *         chain, or {@link Integer#MAX_VALUE} when the budget is unset
     *         (no deadline → unbounded). May be 0 when the budget is fully
     *         consumed; negative values are not produced.
     */
    public static int remainingAttempts() {
        int[] holder = GLOBAL_ATTEMPT_BUDGET.get();
        return holder == null ? Integer.MAX_VALUE : holder[0];
    }

    /**
     * @return {@code true} when at least one more HTTP attempt is allowed
     *         (or no budget is set at all).
     */
    public static boolean canAttempt() {
        return remainingAttempts() > 0;
    }

    /**
     * Atomically consume one HTTP attempt from the shared budget.
     *
     * @return remaining attempts AFTER consumption, or
     *         {@link Integer#MAX_VALUE} when the budget is unset. A return
     *         value of {@code -1} or below indicates the caller asked for an
     *         attempt the budget could not honour — callers should treat
     *         this as "stop now" and surface a deadline-exceeded signal.
     */
    public static int consumeAttempt() {
        int[] holder = GLOBAL_ATTEMPT_BUDGET.get();
        if (holder == null) {
            return Integer.MAX_VALUE;
        }
        return --holder[0];
    }
}
