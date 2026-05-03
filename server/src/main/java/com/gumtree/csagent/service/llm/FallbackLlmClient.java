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
            if (isTransient(e)) {
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
