package com.gumtree.csagent.service.llm;

/**
 * Thread-scoped wall-clock deadline for LLM calls on the current request thread.
 *
 * <p>Sprint 8 §C: any user-facing path that calls an LLM should set a deadline
 * (typically ~30s) at the HTTP entry point. The LLM clients
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
     * Sprint 8.1 §M1 follow-up — per-invocation HTTP-attempt budget for the
     * provider chain on the current request thread. Initialised by
     * {@link #setDeadline(long)} and re-armed by {@link #beginInvocation()} at
     * the start of every logical LLM invocation (i.e. each public entry into
     * {@link FallbackLlmClient#chat}).
     *
     * <p>Sprint 8.1 closure follow-up (2026-05-07): this counter represents
     * the retry / fallback budget for a SINGLE logical LLM invocation, NOT
     * the total number of successful LLM calls allowed in a user turn. The
     * wall-clock {@link #DEADLINE_EPOCH_MILLIS} is the only thing that
     * bounds the whole turn — multiple successful invocations in the same
     * turn (e.g. DISCOVER's {@code search_knowledge} + {@code classify_use_case}
     * followed by a same-turn RESOLVE replan) each re-arm this budget at
     * entry and only decrement it when an attempt is actually issued.
     *
     * <p>With a deadline in effect, total HTTP calls within a single
     * provider-chain invocation are hard-capped at
     * {@link #DEFAULT_INVOCATION_ATTEMPT_BUDGET} (= 2). When no deadline is
     * set (internal / batch / eval / unit-test paths), the counter stays
     * null and the existing per-client retry behaviour stands.
     *
     * <p>Stored as a single-element {@code int[]} so the holder can be
     * mutated in place without re-installing the {@link ThreadLocal} on
     * every decrement.
     */
    private static final ThreadLocal<int[]> INVOCATION_ATTEMPT_BUDGET = new ThreadLocal<>();

    /**
     * Default per-invocation HTTP-attempt budget when a user-facing deadline
     * is set. Sized to fit one primary + one fallback attempt within the
     * 30 s wall-clock budget the controller installs.
     */
    public static final int DEFAULT_INVOCATION_ATTEMPT_BUDGET = 2;

    /**
     * @deprecated retained for source compatibility with prior wording. Use
     * {@link #DEFAULT_INVOCATION_ATTEMPT_BUDGET}. The semantic is now
     * per-invocation rather than per-turn-global.
     */
    @Deprecated
    public static final int DEFAULT_GLOBAL_ATTEMPT_BUDGET = DEFAULT_INVOCATION_ATTEMPT_BUDGET;

    private LlmCallContext() {}

    /**
     * Set the wall-clock deadline for LLM calls on the current thread. Also
     * arms the per-invocation HTTP-attempt budget so the very first
     * provider-chain call has its 2-attempt slot ready.
     */
    public static void setDeadline(long epochMillis) {
        DEADLINE_EPOCH_MILLIS.set(epochMillis);
        INVOCATION_ATTEMPT_BUDGET.set(new int[]{DEFAULT_INVOCATION_ATTEMPT_BUDGET});
    }

    /**
     * Sprint 8.1 closure follow-up (2026-05-07) — re-arm the per-invocation
     * HTTP-attempt budget at the start of each logical LLM invocation
     * (called by {@link FallbackLlmClient#chat}). Successful first-attempt
     * calls do NOT carry their consumed budget into subsequent invocations;
     * only the wall-clock deadline bounds the whole turn.
     *
     * <p>No-op when no deadline is set (internal / batch / eval paths) — the
     * budget stays unbounded.
     */
    public static void beginInvocation() {
        if (DEADLINE_EPOCH_MILLIS.get() == null) {
            return;
        }
        int[] holder = INVOCATION_ATTEMPT_BUDGET.get();
        if (holder == null) {
            INVOCATION_ATTEMPT_BUDGET.set(new int[]{DEFAULT_INVOCATION_ATTEMPT_BUDGET});
        } else {
            holder[0] = DEFAULT_INVOCATION_ATTEMPT_BUDGET;
        }
    }

    /** Clear the deadline. MUST be called in a finally block by whoever set it. */
    public static void clear() {
        DEADLINE_EPOCH_MILLIS.remove();
        INVOCATION_ATTEMPT_BUDGET.remove();
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
     * @return remaining HTTP attempts allowed within the current
     *         provider-chain invocation, or {@link Integer#MAX_VALUE} when
     *         the budget is unset (no deadline → unbounded). May be 0 when
     *         the budget is fully consumed; negative values are not
     *         produced.
     */
    public static int remainingAttempts() {
        int[] holder = INVOCATION_ATTEMPT_BUDGET.get();
        return holder == null ? Integer.MAX_VALUE : holder[0];
    }

    /**
     * @return {@code true} when at least one more HTTP attempt is allowed
     *         within the current provider-chain invocation (or no budget is
     *         set at all).
     */
    public static boolean canAttempt() {
        return remainingAttempts() > 0;
    }

    /**
     * Atomically consume one HTTP attempt from the per-invocation budget.
     *
     * @return remaining attempts AFTER consumption, or
     *         {@link Integer#MAX_VALUE} when the budget is unset. A return
     *         value of {@code -1} or below indicates the caller asked for an
     *         attempt the budget could not honour — callers should treat
     *         this as "stop now" and surface a deadline-exceeded signal.
     */
    public static int consumeAttempt() {
        int[] holder = INVOCATION_ATTEMPT_BUDGET.get();
        if (holder == null) {
            return Integer.MAX_VALUE;
        }
        return --holder[0];
    }
}
