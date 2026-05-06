package com.gumtree.csagent.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Generic OpenAI-compatible chat-completion client. The same class wires either
 * Kimi (primary) or DeepSeek (fallback); the active provider is chosen by the
 * Spring config that constructs the bean (see {@link com.gumtree.csagent.config.LlmClientConfig}).
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final String providerLabel;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(String providerLabel, String apiKey, String baseUrl,
                                     String model, ObjectMapper objectMapper) {
        this.providerLabel = providerLabel;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        // Sprint 8 §A: fail fast — kill the slow tail well below the
        // user-facing budget so a fallback attempt fits in the same request.
        //
        // Sprint 8.1 follow-up #2 (2026-05-06): widened to match the new 30 s
        // user-facing deadline. Healthy chat-completion latency on the new
        // primary (deepseek-v4-flash) is ~1-1.5 s on small payloads but
        // ~3-5 s with the agent's full ~3 k-token system + projection
        // payload. A 12 s read timeout absorbs that comfortably while still
        // killing the genuinely slow tail. The deadline-aware retry loop in
        // {@link #chat} skips the second attempt when the remaining budget
        // cannot fit one full attempt.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);   // 3s — TCP/TLS handshake budget
        factory.setReadTimeout(12000);     // 12s — single LLM completion budget
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
    }

    /**
     * @deprecated Legacy constructor preserved for test-source backward compatibility only.
     *             Production wiring uses the explicit (providerLabel, apiKey, baseUrl, model, objectMapper)
     *             constructor via {@code LlmClientConfig}. This shim picks Kimi if its key is set,
     *             else DashScope — matching the pre-Step-4 behaviour of the deleted in-method fallback.
     */
    @Deprecated
    public OpenAiCompatibleLlmClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this(
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? "kimi" : "dashscope",
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? llmProperties.getKimi().getApiKey() : llmProperties.getDashscope().getApiKey(),
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? llmProperties.getKimi().getBaseUrl() : llmProperties.getDashscope().getBaseUrl(),
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? llmProperties.getKimi().getModel() : llmProperties.getDashscope().getChatModel(),
            objectMapper
        );
    }

    /**
     * Sprint §C1: at most one bounded retry for transient bot-side LLM failures.
     *
     * <p>Transient (retried once): network / connect / read-timeout
     * ({@link RestClientException} that is not a HttpStatusCodeException),
     * HTTP {@code 429} (rate limit), HTTP {@code 5xx} (transient backend).
     *
     * <p>Non-transient (no retry, surfaced immediately to
     * {@link FallbackLlmClient}): HTTP {@code 401/403} (auth / endpoint
     * misconfiguration — handled by {@link com.gumtree.csagent.config.LlmConfigValidator}
     * at startup; no point retrying the same wrong key), other 4xx (malformed
     * request — would not be cured by retry).
     *
     * <p>Cross-provider fallback to DeepSeek is the next layer up
     * ({@link FallbackLlmClient}) and treats 429 / 5xx / network the same way.
     * The result is at most 2 attempts at this provider plus 2 attempts at the
     * fallback — bounded, no busy-loop.
     */
    /**
     * Minimum remaining wall-clock budget required to start another attempt
     * (one full connect + read timeout, plus a small buffer). Used by the
     * deadline-aware retry decision: if the budget falls below this, the
     * retry is skipped in favour of a fast give-up.
     *
     * <p>Sprint 8.1 follow-up #2 (2026-05-06): widened in lockstep with the
     * new connect/read timeouts (3 s + 12 s). Within the 30 s
     * user-facing budget that yields up to two primary attempts (≈ 30 s) OR
     * one primary attempt plus one fallback attempt (≈ 30 s) — bounded.
     */
    private static final long MIN_BUDGET_MS_FOR_NEXT_ATTEMPT = 3000L + 12000L + 200L;

    @Override
    public LlmResponse chat(LlmRequest request) {
        Exception lastException = null;
        // Sprint 8.1 §M1 follow-up (2026-05-06): when a user-facing
        // wall-clock deadline is in effect, the chain runs under a shared
        // 2-attempt budget on {@link LlmCallContext}. The cross-provider
        // hedge is the better retry — same-provider retries usually fail
        // the same way (rate limit / overloaded) — so each inner client
        // gets exactly one attempt and {@link FallbackLlmClient} engages
        // the second provider for the second slot. When no budget is set
        // (internal / batch / eval / unit-test paths) the legacy
        // {@code attempt <= 2} retry stands.
        int maxAttempts = LlmCallContext.remainingAttempts() == Integer.MAX_VALUE ? 2 : 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // Sprint 8 §C / Sprint 8.1 §M1: check the wall-clock deadline
            // before each attempt. If the user's request budget has already
            // been spent, abort with a non-transient
            // {@link LlmDeadlineExceededException} so the caller can render
            // a graceful "we gave up" UX rather than continuing to burn into
            // the retry / fallback chain.
            Long remainingBefore = LlmCallContext.remainingMillis();
            if (remainingBefore != null && remainingBefore <= 0) {
                log.warn("LLM [chat:deadline-exceeded] provider={} attempt={} remaining_budget_ms={} retry_decision=abort",
                        providerLabel, attempt, remainingBefore);
                throw new LlmDeadlineExceededException(
                        "LLM deadline exceeded before attempt " + attempt
                                + " (provider=" + providerLabel + ")");
            }
            // Sprint 8.1 §M1 follow-up (2026-05-06): consume one slot from
            // the shared HTTP-attempt budget BEFORE issuing the call.
            // Returns Integer.MAX_VALUE when no budget is set (legacy /
            // unit-test paths) — the chain remains effectively unbounded
            // there. When the budget is set and already at 0, abort with a
            // deadline-exceeded signal: this fires when the fallback layer
            // has already consumed its slot and the caller is asking for an
            // attempt the chain can no longer honour.
            int remainingAttempts = LlmCallContext.consumeAttempt();
            if (remainingAttempts < 0) {
                log.warn("LLM [chat:global-budget-exhausted] provider={} attempt={} "
                                + "remaining_attempts_after={} retry_decision=abort",
                        providerLabel, attempt, remainingAttempts);
                throw new LlmDeadlineExceededException(
                        "LLM global attempt budget exhausted before attempt " + attempt
                                + " (provider=" + providerLabel + ")");
            }
            long attemptStart = System.currentTimeMillis();
            try {
                return doChat(request, attempt);
            } catch (HttpStatusCodeException e) {
                lastException = e;
                int status = e.getStatusCode().value();
                long elapsed = System.currentTimeMillis() - attemptStart;
                if (!isRetryableStatus(status)) {
                    // Non-transient (auth / 4xx) — fail fast so FallbackLlmClient
                    // can decide whether to engage fallback. Sprint 8.1 §M1:
                    // 401 / 403 / other 4xx are NEVER retried — an unmodified
                    // request will keep failing the same way.
                    log.warn("LLM [chat:non-retryable-status] provider={} attempt={} status={} "
                                    + "elapsed_ms={} failure_class={} retry_decision=no_retry_non_transient",
                            providerLabel, attempt, status, elapsed,
                            classifyHttpStatus(status));
                    throw e;
                }
                if (attempt < maxAttempts && hasBudgetForRetry() && LlmCallContext.canAttempt()) {
                    Long remaining = LlmCallContext.remainingMillis();
                    log.warn("LLM [chat:retry] provider={} attempt={} status={} elapsed_ms={} "
                                    + "failure_class={} remaining_budget_ms={} "
                                    + "remaining_attempts={} retry_decision=retry "
                                    + "sleeping 200ms before retry",
                            providerLabel, attempt, status, elapsed,
                            classifyHttpStatus(status), remaining,
                            LlmCallContext.remainingAttempts());
                    sleepQuietly(200);
                } else if (attempt < maxAttempts) {
                    String reason = LlmCallContext.canAttempt()
                            ? ("retryable_status=" + status)
                            : ("global_budget_exhausted_after_status=" + status);
                    abortRetryDueToDeadline(attempt, reason, e);
                }
            } catch (RestClientException e) {
                lastException = e;
                long elapsed = System.currentTimeMillis() - attemptStart;
                String failureClass = classifyTransport(e);
                if (attempt < maxAttempts && hasBudgetForRetry() && LlmCallContext.canAttempt()) {
                    Long remaining = LlmCallContext.remainingMillis();
                    log.warn("LLM [chat:retry] provider={} attempt={} elapsed_ms={} "
                                    + "failure_class={} remaining_budget_ms={} "
                                    + "remaining_attempts={} retry_decision=retry "
                                    + "sleeping 200ms before retry",
                            providerLabel, attempt, elapsed, failureClass, remaining,
                            LlmCallContext.remainingAttempts());
                    sleepQuietly(200);
                } else if (attempt < maxAttempts) {
                    String reason = LlmCallContext.canAttempt()
                            ? ("transport=" + failureClass)
                            : ("global_budget_exhausted_after_transport=" + failureClass);
                    abortRetryDueToDeadline(attempt, reason, e);
                }
            }
        }
        log.error("LLM [chat:exhausted] provider={} after {} attempts attempt_count={} retry_decision=exhausted",
                providerLabel, maxAttempts, maxAttempts, lastException);
        throw new RuntimeException(
                "LLM API call failed after retry (provider=" + providerLabel + ")", lastException);
    }

    /** Sprint 8.1 §M1 telemetry tag for retryable HTTP statuses. */
    private static String classifyHttpStatus(int status) {
        if (status == 429) return "rate_limited";
        if (status >= 500 && status <= 599) return "server_error_" + status;
        if (status == 401 || status == 403) return "auth_error_" + status;
        return "http_" + status;
    }

    /** Sprint 8.1 §M1 telemetry tag for transport-class failures. */
    private static String classifyTransport(RestClientException e) {
        Throwable cur = e;
        for (int depth = 0; cur != null && depth < 6; depth++, cur = cur.getCause()) {
            if (cur instanceof java.net.SocketTimeoutException) return "read_timeout";
            if (cur instanceof java.net.ConnectException) return "connect_timeout";
            String msg = cur.getMessage() == null ? "" : cur.getMessage().toLowerCase();
            if (msg.contains("read timed out")) return "read_timeout";
            if (msg.contains("connect timed out")) return "connect_timeout";
            if (msg.contains("timeout") || msg.contains("timed out")) return "transport_timeout";
        }
        return "transport_" + e.getClass().getSimpleName();
    }

    /**
     * Sprint 8 §C: a retry is only worth attempting when the remaining
     * deadline can fit one full attempt (connect + read + sleep buffer).
     * Without this check, two back-to-back read timeouts can blow ~16s out
     * of a 10s user budget before the deadline check at the top of the
     * next iteration fires.
     *
     * @return {@code true} when no deadline is set (unbounded path) OR the
     *         remaining budget is at least {@link #MIN_BUDGET_MS_FOR_NEXT_ATTEMPT}.
     */
    private static boolean hasBudgetForRetry() {
        Long remaining = LlmCallContext.remainingMillis();
        if (remaining == null) return true;
        return remaining >= MIN_BUDGET_MS_FOR_NEXT_ATTEMPT;
    }

    private void abortRetryDueToDeadline(int attempt, String reason, Exception cause) {
        Long remaining = LlmCallContext.remainingMillis();
        log.warn("LLM [chat:deadline-skip-retry] provider={} attempt={} remaining={}ms reason={}; "
                + "not retrying, surfacing as deadline-exceeded",
                providerLabel, attempt, remaining, reason);
        LlmDeadlineExceededException ex = new LlmDeadlineExceededException(
                "LLM deadline insufficient for retry after attempt " + attempt
                        + " (provider=" + providerLabel + ", remaining=" + remaining + "ms)");
        ex.initCause(cause);
        throw ex;
    }

    private static boolean isRetryableStatus(int status) {
        return status == 429 || (status >= 500 && status <= 599);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private LlmResponse doChat(LlmRequest request, int attempt) {
        long startTime = System.currentTimeMillis();
        String url = baseUrl + "/chat/completions";

        if (apiKey == null || apiKey.isBlank()) {
            // Sprint §C0: surface configuration failures at the call site too,
            // not just at startup, so unit tests that build the client directly
            // also get a clear diagnostic.
            throw new IllegalStateException("LLM provider " + providerLabel + " has no api-key configured");
        }

        log.info("LLM [chat:request] provider={} model={} url={} attempt={}",
                providerLabel, model, url, attempt);

        try {
            ObjectNode body = buildRequestBody(request, model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            long latencyMs = System.currentTimeMillis() - startTime;
            LlmResponse llmResponse = parseResponse(response.getBody(), latencyMs);
            log.info("LLM [chat:response] provider={} model={} attempt={} latency={}ms tokens={}/{}",
                    providerLabel, model, attempt, latencyMs,
                    llmResponse.getPromptTokens(), llmResponse.getCompletionTokens());
            return llmResponse;

        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            // Sprint §C0 (post Sprint 7.1 credential rotation): 401/403
            // means the configured endpoint / key pair is wrong; the most
            // common failure mode in this repo is the .ai vs .cn Kimi
            // endpoint flip. Make the log line unambiguous so the operator
            // does not chase it as a generic "LLM flake". The body is
            // logged because Moonshot returns a structured error message,
            // never the secret. The api-key is never logged.
            int code = status.value();
            if (code == 401 || code == 403) {
                log.error("LLM [chat:auth-error] provider={} model={} url={} status={} body={} — "
                        + "endpoint/key pair rejected. If provider=kimi, the current working "
                        + "endpoint for this repo's K2.6 provisioning is '{}'. The legacy '{}' "
                        + "endpoint may still hold older keys. Configure KIMI_BASE_URL accordingly.",
                        providerLabel, model, url, code, e.getResponseBodyAsString(),
                        com.gumtree.csagent.config.LlmConfigValidator.KIMI_WORKING_ENDPOINT,
                        com.gumtree.csagent.config.LlmConfigValidator.KIMI_LEGACY_ENDPOINT);
            } else {
                log.error("LLM [chat:http-error] provider={} model={} attempt={} status={} body={}",
                        providerLabel, model, attempt, code, e.getResponseBodyAsString());
            }
            throw e; // Surface to caller; FallbackLlmClient inspects status for transient classification.
        } catch (RestClientException e) {
            throw e; // Let retry loop handle other RestClientException (timeouts, connect failures).
        } catch (Exception e) {
            log.error("LLM [chat:processing-error] provider={} attempt={}: {}",
                    providerLabel, attempt, e.getMessage(), e);
            throw new RuntimeException("Error processing LLM request (provider=" + providerLabel + ")", e);
        }
    }

    private ObjectNode buildRequestBody(LlmRequest request, String model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);

        boolean isKimiK2 = model != null && model.startsWith("kimi-k2");
        if (isKimiK2) {
            // K2.6/K2.5 enforce fixed temperature values; skip it and let the API use its default.
            // Disable thinking for structured output scenarios (JSON actions, routing, reranking).
            ObjectNode thinking = objectMapper.createObjectNode();
            thinking.put("type", "disabled");
            body.set("thinking", thinking);
        } else {
            body.put("temperature", request.getTemperature());
        }

        body.put("max_tokens", request.getMaxTokens());

        if (request.getResponseFormat() != null && !request.getResponseFormat().isBlank()) {
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("type", request.getResponseFormat());
            body.set("response_format", responseFormat);
        }

        ArrayNode messages = objectMapper.createArrayNode();

        // Add system prompt as first message
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messages.add(systemMsg);
        }

        // Add conversation messages
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
                messages.add(msgNode);
            }
        }

        body.set("messages", messages);
        return body;
    }

    private LlmResponse parseResponse(String responseBody, long latencyMs) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            String content = "";
            String finishReason = "";

            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                if (message != null && message.has("content")) {
                    content = message.get("content").asText("");
                }
                if (firstChoice.has("finish_reason")) {
                    finishReason = firstChoice.get("finish_reason").asText("");
                }
            }

            int promptTokens = 0;
            int completionTokens = 0;
            JsonNode usage = root.get("usage");
            if (usage != null) {
                promptTokens = usage.path("prompt_tokens").asInt(0);
                completionTokens = usage.path("completion_tokens").asInt(0);
            }

            return LlmResponse.builder()
                    .content(content)
                    .finishReason(finishReason)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .latencyMs(latencyMs)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
