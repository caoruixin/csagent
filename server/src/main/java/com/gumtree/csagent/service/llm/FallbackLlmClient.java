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
        try {
            return primary.chat(request);
        } catch (Exception e) {
            // Sprint 8 §C: if the wall-clock deadline is already exceeded, do
            // NOT engage the fallback — by definition we no longer have time
            // to wait for a second provider's full attempt. Surface the
            // deadline-exceeded signal so the caller can render a graceful UX.
            if (e instanceof LlmDeadlineExceededException || LlmCallContext.isExceeded()) {
                log.warn("LLM [chat:deadline-exceeded-skipping-fallback] primary={} fallback={} skipped",
                        primaryLabel, fallbackLabel);
                throw e instanceof LlmDeadlineExceededException ? (LlmDeadlineExceededException) e
                        : new LlmDeadlineExceededException(
                                "LLM deadline exceeded after primary=" + primaryLabel
                                        + " failure; not engaging fallback=" + fallbackLabel);
            }
            if (isTransient(e)) {
                // Sprint 8.1 §M1 follow-up (2026-05-06): respect the shared
                // HTTP-attempt budget. When the primary has already consumed
                // every slot the chain is allowed (e.g. one retry inside
                // OpenAiCompatibleLlmClient with a 30 s wall-clock deadline),
                // we MUST NOT engage the fallback — that would push total
                // HTTP calls past 2 and let two 12 s read timeouts blow
                // through the user-facing budget. Surface as
                // {@link LlmDeadlineExceededException} so the caller renders
                // the graceful give-up UX instead of waiting for the fallback.
                if (!LlmCallContext.canAttempt()) {
                    log.warn("LLM [chat:fallback-skipped-budget] primary={} fallback={} "
                                    + "remaining_attempts={}; not engaging fallback after primary "
                                    + "transient failure ({}: {})",
                            primaryLabel, fallbackLabel,
                            LlmCallContext.remainingAttempts(),
                            e.getClass().getSimpleName(), e.getMessage());
                    LlmDeadlineExceededException budgetExhausted = new LlmDeadlineExceededException(
                            "LLM provider chain attempt budget exhausted after primary="
                                    + primaryLabel + " transient failure; "
                                    + "fallback=" + fallbackLabel + " not engaged");
                    budgetExhausted.initCause(e);
                    throw budgetExhausted;
                }
                log.warn("LLM [chat:fallback-engaged] primary={} failed transiently ({}: {}); retrying with fallback={}",
                        primaryLabel, e.getClass().getSimpleName(), e.getMessage(), fallbackLabel);
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
}
