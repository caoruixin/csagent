package com.gumtree.csagent.service.llm;

import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Wraps a primary {@link LlmClient} and falls back to a secondary on transient failures.
 * Non-transient failures (auth, malformed request, parse errors) are re-thrown without
 * engaging the fallback to avoid masking real bugs and burning the fallback quota.
 *
 * Per phase3 §3.8.5: primary=Kimi 2.6, fallback=DeepSeek v4 pro.
 */
@Slf4j
public class FallbackLlmClient implements LlmClient {

    /**
     * Sprint 8.1 closure follow-up (2026-05-07) — minimum remaining
     * wall-clock budget required to engage the fallback. Mirrors the
     * inner-client floor (3 s connect + 12 s read + small buffer) so a
     * fast-failing primary that leaves less than one full attempt's worth
     * of wall-clock cannot drag the chain into a 12 s read-timeout we no
     * longer have time to wait on.
     */
    private static final long MIN_FALLBACK_BUDGET_MS = 3_000L + 12_000L + 200L;

    private final LlmClient primary;
    private final LlmClient fallback;
    private final String primaryLabel;
    private final String fallbackLabel;

    public FallbackLlmClient(LlmClient primary, LlmClient fallback,
                             String primaryLabel, String fallbackLabel) {
        this.primary = primary;
        this.fallback = fallback;
        this.primaryLabel = primaryLabel;
        this.fallbackLabel = fallbackLabel;
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        // Sprint 8.1 closure follow-up (2026-05-07): re-arm the per-invocation
        // attempt budget so multiple successful logical LLM invocations within
        // the same user turn (e.g. DISCOVER's search_knowledge +
        // classify_use_case followed by a same-turn RESOLVE replan) each get
        // a fresh primary + fallback slot. Wall-clock deadline still bounds
        // the whole turn — only the retry counter is per-invocation.
        LlmCallContext.beginInvocation();
        try {
            return primary.chat(request);
        } catch (Exception e) {
            // Sprint 8 §C: if the wall-clock deadline is already exceeded, do
            // NOT engage the fallback — by definition we no longer have time
            // to wait for a second provider's full attempt. Surface the
            // deadline-exceeded signal so the caller can render a graceful UX.
            if (e instanceof LlmDeadlineExceededException || LlmCallContext.isExceeded()) {
                Long remaining = LlmCallContext.remainingMillis();
                log.warn("LLM [chat:fallback-skipped-deadline-exceeded] primary={} fallback={} "
                                + "remaining_budget_ms={} retry_decision=fallback_skipped_deadline_exceeded "
                                + "failure_class={}",
                        primaryLabel, fallbackLabel, remaining,
                        e.getClass().getSimpleName());
                throw e instanceof LlmDeadlineExceededException ? (LlmDeadlineExceededException) e
                        : new LlmDeadlineExceededException(
                                "LLM deadline exceeded after primary=" + primaryLabel
                                        + " failure; not engaging fallback=" + fallbackLabel);
            }
            if (isTransient(e)) {
                // Sprint 8.1 §M1 follow-up — respect the per-invocation
                // HTTP-attempt budget. When the primary has already consumed
                // every slot the chain is allowed (e.g. the inner client's
                // own retry already fired under the 30 s deadline), we MUST
                // NOT engage the fallback — that would push total HTTP calls
                // past 2 and let two 12 s read timeouts blow through the
                // user-facing budget. Surface as
                // {@link LlmDeadlineExceededException} so the caller renders
                // the graceful give-up UX instead of waiting for the fallback.
                if (!LlmCallContext.canAttempt()) {
                    log.warn("LLM [chat:fallback-skipped-budget] primary={} fallback={} "
                                    + "remaining_budget_ms={} remaining_attempts={} "
                                    + "retry_decision=fallback_skipped_attempt_budget_exhausted "
                                    + "failure_class={}; not engaging fallback after primary "
                                    + "transient failure ({}: {})",
                            primaryLabel, fallbackLabel,
                            LlmCallContext.remainingMillis(),
                            LlmCallContext.remainingAttempts(),
                            classifyFailureForTelemetry(e),
                            e.getClass().getSimpleName(), e.getMessage());
                    LlmDeadlineExceededException budgetExhausted = new LlmDeadlineExceededException(
                            "LLM provider chain attempt budget exhausted after primary="
                                    + primaryLabel + " transient failure; "
                                    + "fallback=" + fallbackLabel + " not engaged");
                    budgetExhausted.initCause(e);
                    throw budgetExhausted;
                }
                // Sprint 8.1 closure follow-up (2026-05-07) — wall-clock
                // remaining-budget guard at the fallback boundary. The inner
                // client already gates same-provider retries on
                // MIN_BUDGET_MS_FOR_NEXT_ATTEMPT; mirror that here so a fast
                // transient primary failure that left less than one full
                // attempt's worth of wall-clock cannot still engage a
                // fallback that would block on a 12 s read timeout we no
                // longer have time to wait for.
                Long remainingForFallback = LlmCallContext.remainingMillis();
                if (remainingForFallback != null
                        && remainingForFallback < MIN_FALLBACK_BUDGET_MS) {
                    log.warn("LLM [chat:fallback-skipped-insufficient-budget] primary={} fallback={} "
                                    + "remaining_budget_ms={} min_required_ms={} "
                                    + "remaining_attempts={} "
                                    + "retry_decision=fallback_skipped_insufficient_budget "
                                    + "failure_class={}; not engaging fallback after primary "
                                    + "transient failure ({}: {})",
                            primaryLabel, fallbackLabel,
                            remainingForFallback, MIN_FALLBACK_BUDGET_MS,
                            LlmCallContext.remainingAttempts(),
                            classifyFailureForTelemetry(e),
                            e.getClass().getSimpleName(), e.getMessage());
                    LlmDeadlineExceededException insufficient = new LlmDeadlineExceededException(
                            "LLM remaining wall-clock budget (" + remainingForFallback
                                    + " ms) below full-attempt floor (" + MIN_FALLBACK_BUDGET_MS
                                    + " ms) after primary=" + primaryLabel + " transient failure; "
                                    + "fallback=" + fallbackLabel + " not engaged");
                    insufficient.initCause(e);
                    throw insufficient;
                }
                log.warn("LLM [chat:fallback-engaged] primary={} failed transiently ({}: {}); "
                                + "remaining_budget_ms={} remaining_attempts={} "
                                + "retry_decision=fallback_engaged failure_class={}; "
                                + "retrying with fallback={}",
                        primaryLabel, e.getClass().getSimpleName(), e.getMessage(),
                        LlmCallContext.remainingMillis(),
                        LlmCallContext.remainingAttempts(),
                        classifyFailureForTelemetry(e), fallbackLabel);
                try {
                    LlmResponse resp = fallback.chat(request);
                    log.info("LLM [chat:fallback-success] fallback={} succeeded after primary failure", fallbackLabel);
                    return resp;
                } catch (Exception fe) {
                    log.error("LLM [chat:fallback-failed] both primary={} and fallback={} failed",
                            primaryLabel, fallbackLabel, fe);
                    throw fe instanceof RuntimeException ? (RuntimeException) fe
                            : new RuntimeException("Fallback LLM also failed", fe);
                }
            }
            log.error("LLM [chat:non-transient] primary={} failed non-transiently; not retrying with fallback",
                    primaryLabel, e);
            throw e instanceof RuntimeException ? (RuntimeException) e
                    : new RuntimeException("Primary LLM failed (non-transient)", e);
        }
    }

    /**
     * Conservative classifier: connection / timeout / 5xx / 429 are transient.
     * Authentication / 4xx other than 429 / serialization errors are not.
     *
     * Note: OpenAiCompatibleLlmClient wraps RestClientException in a generic
     * RuntimeException after its own internal retry loop, so we walk the cause chain
     * to find the real root.
     */
    private boolean isTransient(Throwable e) {
        Throwable cur = e;
        for (int depth = 0; cur != null && depth < 8; depth++, cur = cur.getCause()) {
            if (cur instanceof HttpStatusCodeException hse) {
                int status = hse.getStatusCode().value();
                return status == 429 || (status >= 500 && status <= 599);
            }
            if (cur instanceof java.net.ConnectException) return true;
            if (cur instanceof java.net.SocketTimeoutException) return true;
            if (cur instanceof org.springframework.web.client.ResourceAccessException) return true;
            // org.springframework.web.client.RestClientException catch-all (timeouts wrapped here too)
            String msg = cur.getMessage() == null ? "" : cur.getMessage().toLowerCase();
            if (msg.contains("timeout") || msg.contains("timed out")) return true;
            if (msg.contains("connection") && (msg.contains("reset") || msg.contains("refused"))) return true;
            if (msg.contains("429") || msg.contains("rate limit") || msg.contains("rate_limit")) return true;
            if (msg.contains("503") || msg.contains("502") || msg.contains("504") || msg.contains("500")) return true;
            // IOException is transient at the very bottom of the chain only.
            if (cur instanceof java.io.IOException && cur.getCause() == null) return true;
        }
        return false;
    }

    /**
     * Sprint 8.1 closure follow-up — coarse failure-class telemetry tag for
     * fallback-skipped log lines. Walks the cause chain in the same shape as
     * {@link #isTransient} so the surfaced tag matches the retry decision.
     */
    private static String classifyFailureForTelemetry(Throwable e) {
        Throwable cur = e;
        for (int depth = 0; cur != null && depth < 8; depth++, cur = cur.getCause()) {
            if (cur instanceof HttpStatusCodeException hse) {
                int status = hse.getStatusCode().value();
                if (status == 429) return "rate_limited";
                if (status >= 500 && status <= 599) return "server_error_" + status;
                return "http_" + status;
            }
            if (cur instanceof java.net.SocketTimeoutException) return "read_timeout";
            if (cur instanceof java.net.ConnectException) return "connect_timeout";
            String msg = cur.getMessage() == null ? "" : cur.getMessage().toLowerCase();
            if (msg.contains("read timed out")) return "read_timeout";
            if (msg.contains("connect timed out")) return "connect_timeout";
            if (msg.contains("timeout") || msg.contains("timed out")) return "transport_timeout";
        }
        return "unknown";
    }
}
